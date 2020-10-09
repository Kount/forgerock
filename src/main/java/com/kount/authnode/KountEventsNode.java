package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.inject.Inject;

import org.forgerock.http.handler.HttpClientHandler;
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
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.EncryptAction;

/**
 * @author reshma.madan
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, 
configClass = KountEventsNode.Config.class, tags = {
"risk" })
public class KountEventsNode extends SingleOutcomeNode {
	private final Logger logger = LoggerFactory.getLogger(KountEventsNode.class);
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
			// domain
			return "Server URL";
		}
	}	
	
	
	@Inject
	public KountEventsNode(@Assisted Config config) {
		this.config = config;
	}	
	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		logger.debug("Kount Login Events started");
		String userHandle = context.sharedState.get(USERNAME).asString();
		if(userHandle!=null) {
			AMIdentity id = IdUtils.getIdentity(userHandle, context.sharedState.get(REALM).asString());
			context.sharedState.put("API_KEY", config.API_KEY());
			callEventsAPI(context,id);
		}else {
			throw new NodeProcessException("User Name is empty");
		}
		return goToNext().replaceSharedState(context.sharedState).build();
	}
	
	/**
	 * @param context
	 * @param id 
	 * @throws NodeProcessException
	 */
	private void callEventsAPI(TreeContext context, AMIdentity id) throws NodeProcessException {
		try {
			HttpURLConnection connection=gePostConnection(context,id);
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
						context.sharedState.put("kountEventsResponseBody", builder.toString());
					}
		} catch (Exception e) {
			logger.error("Unable to get TMX response for session: ");
			throw new NodeProcessException(e);
		}
	}
	/**
	 * @param context
	 * @return
	 */
	private HttpURLConnection gePostConnection(TreeContext context, AMIdentity identity) {
		String ip="";
		HttpURLConnection connection = null;
		try {

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
				osw.write("{\r\n  \"failedAttempt\": {\r\n \"clientId\": \""+context.sharedState.get("kountMerchant").asString()+"\",\r\n\"sessionId\": \""+context.sharedState.get("kountSession").asString()+"\",\r\n\"username\": \""+context.sharedState.get(USERNAME).asString()+"\",\r\n\"userPassword\": \""+encryptedPwd.run().toString()+"\",\r\n\"userIp\": \""+ip+"\"\r\n}\r\n}");
				osw.flush();
				osw.close();
				os.close();  //don't forget to close the OutputStream
				connection.connect();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return connection;

	}

}
