package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static com.kount.authnode.KountLoginNode.UserType.ALLOW;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.http.Handler;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.AuthorizationHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.EncryptAction;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * A node that locks a user account (sets the user as "inactive").
 */
/**
 * @author reshma.madan
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, 
configClass = KountLoginNode.Config.class, tags = {
"risk" })
public class KountLoginNode extends SingleOutcomeNode {

	private final Logger logger = LoggerFactory.getLogger(KountLoginNode.class);
	private final Config config;

	/**
	 * Configuration for the node.
	 */
	public interface Config {
		@Attribute(order = 100, requiredValue = true)
		default String API_KEY() {
			return "API Key";
		}

		@Attribute(order = 200, requiredValue = true)
		default String domain() {
			return "Server URL";
		}

		@Attribute(order = 300, requiredValue = true)
		default String login() {
			return "Login";
		}

		@Attribute(order = 400,requiredValue = true)
		default UserType userType() {
			return ALLOW;
		}

	}
	public enum UserType {

		ALLOW,

		BLOCK,

		CHALLENGE
	}
	/**
	 * Guice constructor.
	 *
	 * @param coreWrapper             A core wrapper instance.
	 * @param identityUtils           An instance of the IdentityUtils.
	 * @param amAccountLockoutFactory factory for generating account lockout
	 *                                objects.
	 * @param config                  The config for this instance.
	 */
	@Inject
	public KountLoginNode(@Assisted Config config, HttpClientHandler client, @Named("HttpClientHandler") Handler handler) {
		this.config = config;
	}

	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		logger.debug("Kount Login Node started");
		String userHandle = context.sharedState.get(USERNAME).asString();
		if(userHandle!=null) {
			AMIdentity id = IdUtils.getIdentity(userHandle, context.sharedState.get(REALM).asString());
			getKountLoginRequest(context, id);
		}
		context.sharedState.put("API_KEY", config.API_KEY());
		return goToNext().replaceSharedState(context.sharedState).build();
	}

	private void getKountLoginRequest(TreeContext context, AMIdentity identity) throws NodeProcessException {
		JsonValue sharedState = context.sharedState;
		if(config.userType()!=null && !config.userType().toString().isEmpty()) {
			sharedState.add("userType", config.userType().toString());
		}
		try {

			OkHttpClient client = new OkHttpClient().newBuilder().build();
			Request request=getRequest(context);
			Response response = client.newCall(request).execute();
			if(response.code()==200) {
				context.sharedState.put("KountLoginApiResponse", response);
				context.sharedState.put("kountLoginResponseBody", response.body().string());
			}
		} catch (Exception e) {
			logger.error("Unable to get TMX response for session: ");
			throw new NodeProcessException(e);
		}
	}
	private Request getRequest(TreeContext context) {
		String ip="";
		String mail = null;
		Iterator itor = null;
		String userHandle = context.sharedState.get(USERNAME).asString();
		AMIdentity identity = IdUtils.getIdentity(userHandle, context.sharedState.get(REALM).asString());
		try {
			Set emails = identity.getAttribute("mail");
			if (emails != null && !emails.isEmpty()) {
				itor = emails.iterator();
				mail = (String) itor.next();
			}
		} catch (SSOException | IdRepoException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		JsonValue transientState = context.transientState;
		EncryptAction encryptedPwd=new EncryptAction(transientState.get(PASSWORD).asString());
		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType,"{\r\n\"clientId\": \""+context.sharedState.get("kountMerchant").asString()+"\",\r\n\"sessionId\": \""+context.sharedState.get("kountSession").asString()+"\",\r\n\"userId\": \""+mail+"\",\r\n\"userPassword\": \""+encryptedPwd.run().toString()+"\",\r\n\"userType\": \""+config.userType().toString()+"\",\r\n\"username\": \""+context.sharedState.get(USERNAME).asString()+"\"\r\n}");
		Request request = new Request.Builder()
				.url("https://"+config.domain()+"/" +config.login())
				.method("POST", body)
				.addHeader("Authorization", "Bearer "+config.API_KEY())
				.addHeader("Content-Type", "application/json")
				.build();
		return request;
	}


}

