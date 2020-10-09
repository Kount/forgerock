package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static com.kount.authnode.KountLoginNode.UserType.ALLOW;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;
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

		@Attribute(order = 300,requiredValue = true)
		default String unique_Identifier() {
			return "Unique Identifier";
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
	/*
	 * @Inject public KountLoginNode(@Assisted Config config, HttpClientHandler
	 * client, @Named("HttpClientHandler") Handler handler) { this.config = config;
	 * }
	 */
	@Inject
	public KountLoginNode(@Assisted Config config) {
		this.config = config;
	}

	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		logger.debug("Kount Login Node started");
		String userHandle = context.sharedState.get(USERNAME).asString();
		if(userHandle!=null) {
			AMIdentity id = IdUtils.getIdentity(userHandle, context.sharedState.get(REALM).asString());
			getKountLoginRequest(context, id);
		}else {
			throw new NodeProcessException("User Name is empty");
		}
		context.sharedState.put("API_KEY", config.API_KEY());
		return goToNext().replaceSharedState(context.sharedState).build();
	}

	void getKountLoginRequest(TreeContext context, AMIdentity identity) throws NodeProcessException {
		JsonValue sharedState = context.sharedState;
		if(config.userType()!=null && !config.userType().toString().isEmpty()) {
			sharedState.add("userType", config.userType().toString());
		}else {
			throw new NodeProcessException("User Type is empty");
		}
		try {

			HttpURLConnection connection=gePostConnection(context,identity);
			// This line makes the request
			InputStreamReader in = new InputStreamReader((InputStream) connection.getContent());
			BufferedReader buff = new BufferedReader(in);
			String line;
			StringBuilder builder = new StringBuilder();
			do {
				line = buff.readLine();
				builder.append(line).append("\n");
			} while (line != null);
			buff.close();

			if(connection.getResponseCode()==200) {
				context.sharedState.put("KountLoginApiResponse", builder);
				context.sharedState.put("kountLoginResponseBody", builder.toString());
			}else {
				throw new NodeProcessException("Http response code is not as Expected");
			}
		} catch (Exception e) {
			logger.error("Unable to get TMX response for session: ");
			throw new NodeProcessException(e);
		}
	}

	private HttpURLConnection gePostConnection(TreeContext context, AMIdentity identity) throws NodeProcessException {
		String ip="";
		String userId = null;
		Iterator itor = null;
		HttpURLConnection connection = null;
		try {
			Set userIds = identity.getAttribute(config.unique_Identifier());
			if (userIds != null && !userIds.isEmpty()) {
				itor = userIds.iterator();
				userId = (String) itor.next();
			}else {
				throw new NodeProcessException("UserId id empty");
			}
				JsonValue transientState = context.transientState;
				EncryptAction encryptedPwd=new EncryptAction(transientState.get(PASSWORD).asString());
				String uri="https://"+config.domain();
				URL url = new URL(uri);
				connection = (HttpURLConnection) url.openConnection();
				// Now it's "open", we can set the request method, headers etc.
				connection.setRequestProperty("accept", "application/json");
				connection.setRequestProperty ("Authorization", "Bearer "+config.API_KEY());
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				OutputStream os = connection.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");    
				osw.write("{\r\n\"clientId\": \""+context.sharedState.get("kountMerchant").asString()+"\",\r\n\"sessionId\": \""+context.sharedState.get("kountSession").asString()+"\",\r\n\"userId\": \""+userId+"\",\r\n\"userPassword\": \""+encryptedPwd.run().toString()+"\",\r\n\"userType\": \""+config.userType().toString()+"\",\r\n\"username\": \""+context.sharedState.get(USERNAME).asString()+"\"\r\n}");
				osw.flush();
				osw.close();
				os.close();  //don't forget to close the OutputStream
				connection.connect();

		} catch (SSOException | IdRepoException  |IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return connection;

	}



}

