package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.kount.authnode.HttpConnection.HTTPResponse;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;


/**
 * The KountLoginNode makes the call to the Kount Control Login API, which is
 * the service that will make the risk decision based on the data Kount has
 * collected. The response will be either Allow, Block or Challenge.
 * 
 * @author reshma.madan
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = KountLoginNode.Config.class, tags = {
		"risk" })
public class KountLoginNode extends SingleOutcomeNode {

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(KountLoginNode.class);

	/** The config. */
	private final Config config;

	/** The http connection. */
	private final HttpConnection httpConnection = new HttpConnection();

	/**
	 * Configuration for the node.
	 */
	public interface Config {

		/**
		 * Api key.
		 *
		 * @return the char[]
		 */
		@Attribute(order = 100, requiredValue = true)
		@Password
		char[] apiKey();

		/**
		 * Login domain option.
		 *
		 * @return the login domain option
		 */
		@Attribute(order = 200, requiredValue = true)
		default LoginDomainOption loginDomainOption() {
			return LoginDomainOption.SANDBOX;
		}

		/**
		 * Unique identifier.
		 *
		 * @return the string
		 */
		default @Attribute(order = 300, requiredValue = true) String uniqueIdentifier() {
			return Constants.KOUNT_LOGIN_UNIQUE_IDENTIFIER;
		}
	}

	/**
	 * The Enum LoginDomainOption.
	 */
	public enum LoginDomainOption {

		PRODUCTION(Constants.KOUNT_PRODUCTION_SERVER + Constants.KOUNT_LOGIN_API_ENDPOINT),

		SANDBOX(Constants.KOUNT_SANDBOX_SERVER + Constants.KOUNT_LOGIN_API_ENDPOINT);

		String domainOption;

		LoginDomainOption(String domainOption) {
			this.domainOption = domainOption;
		}
	}

	/**
	 * Guice constructor.
	 *
	 * @param config The config for this instance.
	 */
	@Inject
	public KountLoginNode(@Assisted Config config) {
		this.config = config;
	}

	/**
	 * Process.
	 *
	 * @param context the context
	 * @return the action
	 * @throws NodeProcessException the node process exception
	 */
	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		logger.info("Kount Login Node started");
		String userHandle = context.sharedState.get(USERNAME).asString();
		if (userHandle != null) {
			AMIdentity id = IdUtils.getIdentity(userHandle, context.sharedState.get(REALM).asString());
			getKountLoginRequest(context, id);
		} else {
			logger.error("ERROR: KountLoginNode.process(), Message: userHandle is null !!!");
			throw new NodeProcessException("User Name is empty");
		}
		context.sharedState.put("API_KEY", String.valueOf(config.apiKey()));
		return goToNext().replaceSharedState(context.sharedState).build();
	}

	/**
	 * Gets the kount login request.
	 *
	 * @param context  the context
	 * @param identity the identity
	 * @return the kount login request
	 * @throws NodeProcessException the node process exception
	 */
	void getKountLoginRequest(TreeContext context, AMIdentity identity) throws NodeProcessException {
		logger.debug("In KountLoginNode.getKountLoginRequest()");
		try {
			HttpURLConnection connection = loginAPIPost(context, identity);
			HTTPResponse httpResponse = httpConnection.parseResponse(connection);
			String loginDecisionCorrelationId = connection.getHeaderField("X-Correlation-ID");
			context.sharedState.put("loginDecisionCorrelationId", loginDecisionCorrelationId);
			if (httpResponse != null && httpResponse.getResponseCode() == 200) {
				logger.info("KountLoginNode.getKountLoginRequest(): Post Request Success, status code:"
						+ httpResponse.getResponseCode());
				context.sharedState.put("KountLoginApiResponse", httpResponse.getBuilder());
				context.sharedState.put("kountLoginResponseBody", httpResponse.getBuilder().toString());
			} else {
				logger.error("ERROR: KountLoginNode.getKountLoginRequest(), Message: userHandle is null !!!");
				throw new NodeProcessException("Http response code is not as Expected");
			}
		} catch (Exception e) {
			logger.error(
					"ERROR: KountLoginNode.getKountLoginRequest(), Unable to get TMX response for session, Message:"
							+ e.getMessage());
			throw new NodeProcessException(e);
		}
	}

	/**
	 * Login API post.
	 *
	 * @param context  the context
	 * @param identity the identity
	 * @return the http URL connection
	 * @throws NodeProcessException the node process exception
	 */
	private HttpURLConnection loginAPIPost(TreeContext context, AMIdentity identity) throws NodeProcessException {
		logger.debug("In KountLoginNode.loginAPIPost()");
		String userId;
		Iterator<String> itor;
		HttpURLConnection connection = null;
		try {
			Set<String> userIds = identity.getAttribute(config.uniqueIdentifier());
			if (userIds != null && !userIds.isEmpty()) {
				itor = userIds.iterator();
				userId = itor.next();
			} else {
				logger.error("ERROR: KountLoginNode.loginAPIPost(), Message: userIds is null !!!");
				throw new NodeProcessException("UserId id empty");
			}

			String payload = getRequestPayload(context, userId);
			connection = httpConnection.post(config.loginDomainOption().domainOption, payload,
					String.valueOf(config.apiKey()));

		} catch (SSOException | IdRepoException | JSONException e) {
			logger.error("ERROR: KountLoginNode.loginAPIPost(), Unable to post Login API, Message:" + e.getMessage());
			e.printStackTrace();
		}
		return connection;
	}

	/**
	 * Helper method to generate request payload.
	 *
	 * @param context the context
	 * @param userId  the user id
	 * @return the payload
	 * @throws JSONException the JSON exception
	 */
	private String getRequestPayload(TreeContext context, String userId) throws JSONException {
		logger.debug("In KountLoginNode.getRequestPayload()");
		JSONObject payload = new JSONObject();
		payload.put("clientId", context.sharedState.get("kountMerchant").asString());
		payload.put("sessionId", context.sharedState.get("kountSession").asString());
		payload.put("userId", userId);
		payload.put("userPassword", "");
		payload.put("username", context.sharedState.get(USERNAME).asString());
		return payload.toString();
	}
}
