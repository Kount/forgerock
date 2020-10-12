package com.kount.authnode;

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
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;


/**
 * @author reshma.madan
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, 
configClass = KountTrustedDeviceNode.Config.class, tags = {
"risk" })
public class KountTrustedDeviceNode extends SingleOutcomeNode {

	private final Logger logger = LoggerFactory.getLogger(KountTrustedDeviceNode.class);
	private final Config config;
	String decision="",matchedToDevice="",deviceId="",trustedDeviceState="";
	/**
	 * Configuration for the node.
	 */
	public interface Config {
		@Attribute(order = 200, requiredValue = true)
		default String domain() {
			// domain
			return "Server URL";
		}
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
	public KountTrustedDeviceNode(@Assisted Config config) {
		this.config = config;
	}


	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		// TODO Auto-generated method stub
		logger.debug("Kount Trusted Device started");

		Boolean definedCheck  = context.sharedState.isDefined("kountLoginResponseBody");
		if(definedCheck) {
			JsonValue loginApiResponseBody= context.sharedState.get("kountLoginResponseBody");
			loginApiResponseBody.getObject();
			try {
				JSONObject json = new JSONObject(loginApiResponseBody.asString());
				if(json!=null) {
					decision=json.get("decision").toString();
					deviceId=json.get("deviceId").toString();
					AddTrustedDevice(context,decision,deviceId);
				}
			} catch (JSONException | DevicePersistenceException e) {
				throw new NodeProcessException("Kount Login Response is null");
			}
		}
		else
		{
			throw new NodeProcessException("Kount Login Response is null");
		}

		return goToNext().replaceSharedState(context.sharedState).build();
	}

	private void AddTrustedDevice(TreeContext context, String decision, String deviceId) throws NodeProcessException, DevicePersistenceException {
		if("Challenge".equalsIgnoreCase(decision) ) {
			trustedDeviceState="UNASSIGNED";
		}else if("Block".equalsIgnoreCase(decision) ) {
			trustedDeviceState="BANNED";
		}else {
			trustedDeviceState="TRUSTED";
		}
		try {

			StringBuilder builder;
			//Read device
			HttpURLConnection readDeviceConnection=readDeivceConnection(context, deviceId);
			HttpURLConnection connection=postTrustedDevice(context);
			//if the device is not present the response code will be 404 or greater
			if(readDeviceConnection.getResponseCode()==404) {
				//Add the device
				builder = AddDevice(connection);
				if(connection.getResponseCode()==200) {
					context.sharedState.put("addTrusedDeiceiResponse", builder);
					context.sharedState.put("addTrusedDeiceiResponseBody", builder.toString());
				}
			}
		} catch (Exception e) {
			logger.error("Unable to get TMX response for session: ");
			throw new NodeProcessException(e);
		}
	}


	private StringBuilder AddDevice(HttpURLConnection connection2) throws IOException {
		StringBuilder builder = new StringBuilder();
		if(connection2.getResponseCode()==200) {
			InputStreamReader in = new InputStreamReader((InputStream) connection2.getContent());
			BufferedReader buff = new BufferedReader(in);
			String line;
			do {
				line = buff.readLine();
				builder.append(line).append("\n");
			} while (line != null);
			buff.close();
		}
		return builder;
	}


	private HttpURLConnection postTrustedDevice(TreeContext context) {
		HttpURLConnection connection = null;
		try {
			String uri="https://"+config.domain();
			URL url = new URL(uri);
			connection = (HttpURLConnection) url.openConnection();
			// Now it's "open", we can set the request method, headers etc.
			HttpConnection.setUpHttpPostConnection(connection, context.sharedState.get("API_KEY").toString());
			OutputStream os = connection.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");    
			osw.write("{\r\n\"clientId\": \""+context.sharedState.get("kountMerchant").asString()+"\",\r\n\"sessionId\": \""+context.sharedState.get("kountSession").asString()+"\",\r\n\"userId\": \""+context.sharedState.get(USERNAME).asString()+"\",\r\n\"trustState\": \""+trustedDeviceState+"\"\r\n}");
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


	private HttpURLConnection readDeivceConnection(TreeContext context, String deviceId2) throws IOException {
		String str1 = "Bearer ";
		String str2 = context.sharedState.get("API_KEY").toString();
		String str22=str2.substring(1, str2.length()-1);
		String bearerToken = str1.concat(str22);

		String urlstr = "https://api-qa06.kount.com/trusted-device/devices/"+deviceId+"/clients/"+context.sharedState.get("kountMerchant").asString()+"/users";
		URL url = new URL(urlstr);
		HttpURLConnection	connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty ("Authorization",bearerToken);

		return connection;
	}

}
