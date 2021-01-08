package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.validation.URLValidator;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.kount.authnode.HttpConnection.HTTPResponse;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The KountEventsNode is a single-outcome node which should be used to record a
 * failed login or a failed MFA check, which feeds into Kount’s service to help
 * inform future risk inquiries.
 * 
 * @author reshma.madan
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = KountEventsNode.Config.class, tags = {
		"risk" })
public class KountEventsNode extends SingleOutcomeNode {

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(KountEventsNode.class);

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
		 * Events domain option.
		 *
		 * @return the events domain option
		 */
		default @Attribute(order = 200, validators = {
				RequiredValueValidator.class }) EventsDomainOption eventsDomainOption() {
			return EventsDomainOption.SANDBOX;
		}

		/**
		 * Authentication Outcome
		 * 
		 * @return the AuthenticationOutcome
		 */
		@Attribute(order = 300, requiredValue = true)
		default AuthenticationOutcome authenticationOutcome() {
			return AuthenticationOutcome.Failed;
		}

		/**
		 * Authentication Type
		 * 
		 * @return the authenticationType
		 */
		@Attribute(order = 400, requiredValue = true)
		default AuthenticationType authenticationType() {
			return AuthenticationType.Login;
		}

		/**
		 * Challenge Outcome
		 * 
		 * @return the ChallengeOutcome
		 */
		@Attribute(order = 500, requiredValue = true)
		default ChallengeType challengeType() {
			return ChallengeType.NA;
		}

		/**
		 * Login URL
		 * 
		 * @return String
		 */
		@Attribute(order = 600, requiredValue = true)
		default String loginUrl() {
			return Constants.LOGIN_URL;
		}
	}

	/**
	 * The Enum AuthenticationOutcome.
	 */
	public enum AuthenticationOutcome {
		Success, Failed;
	}

	/**
	 * The Enum AuthenticationType.
	 */
	public enum AuthenticationType {
		Login, Challenge;
	}

	/**
	 * The Enum ChallengeType.
	 */
	public enum ChallengeType {
		NA, Captcha2, Puzzle, SecretQuestion, EmailLink, EmailPIN, TextPIN, TextLink, MFAAppGoogle, MFAAppDuo,
		MFAAppiOS, MFAAppAuthy, MFAAppMicrosoft, MFAAppLastPass, MFAAppOther
	}

	/**
	 * The Enum EventsDomainOption.
	 */
	public enum EventsDomainOption {

		PRODUCTION(Constants.KOUNT_PRODUCTION_SERVER + Constants.KOUNT_EVENTS_API_ENDPOINT),
		SANDBOX(Constants.KOUNT_SANDBOX_SERVER + Constants.KOUNT_EVENTS_API_ENDPOINT);

		String domainOption;

		EventsDomainOption(String domainOption) {
			this.domainOption = domainOption;
		}
	}

	/**
	 * Instantiates a new kount events node.
	 *
	 * @param config the config
	 */
	@Inject
	public KountEventsNode(@Assisted Config config) {
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
		logger.info("Kount Events Node started");
		String userHandle = context.sharedState.get(USERNAME).asString();
		if (userHandle != null) {
			AMIdentity id = IdUtils.getIdentity(userHandle, context.sharedState.get(REALM).asString());
			context.sharedState.put("API_KEY", String.valueOf(config.apiKey()));
			callEventsAPI(context, id);
		} else {
			logger.error("ERROR: KountEventsNode.process(), Message: userHandle is null !!!");
			throw new NodeProcessException("User Name is empty");
		}
		return goToNext().replaceSharedState(context.sharedState).build();
	}

	/**
	 * Call events API.
	 *
	 * @param context the context
	 * @param id      the id
	 * @throws NodeProcessException the node process exception
	 */
	private void callEventsAPI(TreeContext context, AMIdentity id) throws NodeProcessException {
		logger.debug("In KountEventsNode.callEventsAPI()");
		try {
			HttpURLConnection connection;
			if (config.authenticationType().toString().equalsIgnoreCase(Constants.KOUNT_EVENTS_CONFIG_CHALLENGE)) {
				connection = postEventsChallengeFailedAPI(context, id);
			} else {
				connection = postEventsLoginFailedAPI(context, id);
			}
			HTTPResponse httpResponse = httpConnection.parseResponse(connection);

			if (httpResponse != null && httpResponse.getResponseCode() == 200) {
				logger.info("KountEventsNode.callEventsAPI(): Post Request Success, status code: "
						+ httpResponse.getResponseCode());
				context.sharedState.put("kountEventsResponseBody", httpResponse.getBuilder().toString());
			} else {

				if (httpResponse != null) {
					logger.error("ERROR: KountEventsNode.callEventsAPI(), Message: The status code is: "
							+ httpResponse.getResponseCode());
				}
				throw new NodeProcessException("callEventsAPI Http response code is not as Expected");
			}
		} catch (Exception e) {
			logger.error("KountEventsNode.callEventsAPI(), Message: Unable to get TMX response for session");
			e.printStackTrace();
			throw new NodeProcessException(e);
		}
	}

	/**
	 * Post Events Login Failure API
	 * 
	 * @param context  the context
	 * @param identity the AMIdentity
	 * @return the HttpURPConnection
	 */
	private HttpURLConnection postEventsLoginFailedAPI(TreeContext context, AMIdentity identity) {
		logger.debug("In KountEventsNode.postEventsLoginFailedAPI()");
		HttpURLConnection connection = null;
		try {
			String payload = getLoginFailedRequestPayload(context);
			connection = httpConnection.post(config.eventsDomainOption().domainOption, payload,
					String.valueOf(config.apiKey()));
		} catch (Exception e) {
			logger.error(
					"KountEventsNode.postEventsLoginFailedAPI(), Unable to post Event API, Message:" + e.getMessage());
			e.printStackTrace();
		}
		return connection;
	}

	/**
	 * Post Events Challenge Failure API
	 * 
	 * @param context  the context
	 * @param identity the AMIdentity
	 * @return the HttpURPConnection
	 */
	private HttpURLConnection postEventsChallengeFailedAPI(TreeContext context, AMIdentity identity) {
		logger.debug("In KountEventsNode.postEventsChallengeFailedAPI()");
		HttpURLConnection connection = null;

		JsonValue jsonValue = context.sharedState.get(Constants.SET_TIMESTAMP);
		String sentTimestamp = "";
		String completedTimestamp = "";

		if (jsonValue != null) {
			Long startEpochTime = jsonValue.asLong();
			sentTimestamp = formatTimeStamp(startEpochTime);
		}

		jsonValue = context.sharedState.get(Constants.COMPLETED_TIMESTAMP);
		if (jsonValue != null) {
			Long completeTime = jsonValue.asLong();
			completedTimestamp = formatTimeStamp(completeTime);
		}

		try {
			String payload = getChallengeFailedRequestPayload(context, sentTimestamp, completedTimestamp);
			connection = httpConnection.post(config.eventsDomainOption().domainOption, payload,
					String.valueOf(config.apiKey()));

		} catch (Exception e) {
			logger.error("KountEventsNode.postEventsChallengeFailedAPI(), Unable to post Event API, Message:"
					+ e.getMessage());
			e.printStackTrace();
		}
		return connection;

	}

	/**
	 * The helper method to format the epoach time to required Timestamp.
	 * 
	 * @param epochTime
	 * @return the string of formatted time
	 */
	public String formatTimeStamp(Long epochTime) {
		String formattedTime = "";
		logger.debug("In KountEventsNode.formatTimeStamp()");
		if (epochTime != null) {
			Date date = new Date(epochTime);
			DateFormat formatter = new SimpleDateFormat(Constants.UTC_TIMESTAMP, Locale.ENGLISH);
			formattedTime = formatter.format(date);
		}
		return formattedTime;
	}

	/**
	 * Helper method to generate request payload.
	 * 
	 * @param context
	 * @return the payload
	 * @throws JSONException
	 */
	private String getChallengeFailedRequestPayload(TreeContext context, String sentTimestamp,
			String completedTimestamp) throws JSONException {
		logger.debug("In KountEventsNode.getRequestPayload()");
		JSONObject nestedPayload = new JSONObject();
		nestedPayload.put("loginDecisionCorrelationId",
				context.sharedState.get("loginDecisionCorrelationId").asString());
		nestedPayload.put("clientId", context.sharedState.get("kountMerchant").asString());
		nestedPayload.put("sessionId", context.sharedState.get("kountSession").asString());
		nestedPayload.put("username", context.sharedState.get(USERNAME).asString());
		nestedPayload.put("challengeType", config.challengeType());
		nestedPayload.put("challengeStatus", config.authenticationOutcome());
		nestedPayload.put("sentTimestamp", sentTimestamp);
		nestedPayload.put("completedTimestamp", completedTimestamp);
		nestedPayload.put("userId", context.sharedState.get(USERNAME).asString());

		JSONObject payload = new JSONObject();
		payload.put("challengeOutcome", nestedPayload);

		return payload.toString();
	}

	/**
	 * Helper method to generate request payload.
	 * 
	 * @param context
	 * @return the payload
	 * @throws JSONException
	 */
	private String getLoginFailedRequestPayload(TreeContext context) throws JSONException {
		logger.debug("In KountEventsNode.getLoginFailedRequestPayload()");
		JSONObject nestedPayload = new JSONObject();
		nestedPayload.put("clientId", context.sharedState.get("kountMerchant").asString());
		nestedPayload.put("sessionId", context.sharedState.get("kountSession").asString());
		nestedPayload.put("username", context.sharedState.get(USERNAME).asString());
		nestedPayload.put("loginUrl", config.loginUrl());
		nestedPayload.put("userId", context.sharedState.get(USERNAME).asString());

		JSONObject payload = new JSONObject();
		payload.put("failedAttempt", nestedPayload);

		return payload.toString();
	}
}
