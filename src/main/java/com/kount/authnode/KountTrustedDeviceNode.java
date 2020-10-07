package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.inject.Inject;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

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
	public KountTrustedDeviceNode(@Assisted Config config, HttpClientHandler client) {
		this.config = config;
	}


	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		// TODO Auto-generated method stub
		logger.debug("Kount Trusted Device started");
		JsonValue loginApiResponseBody= context.sharedState.get("kountLoginResponseBody");
		if(loginApiResponseBody!=null) {
		loginApiResponseBody.getObject();
		}
		try {
			JSONObject json = new JSONObject(loginApiResponseBody.asString());
			decision=json.get("decision").toString();
			deviceId=json.get("deviceId").toString();
			AddTrustedDevice(context,decision,deviceId);
		} catch (JSONException | NodeProcessException | DevicePersistenceException e) {
			logger.debug("Exception in Kount Trusted Device "+e);
			e.printStackTrace();
		}
		return goToNext().replaceSharedState(context.sharedState).build();
	}

	private void AddTrustedDevice(TreeContext context, String decision, String deviceId) throws NodeProcessException, DevicePersistenceException {
		if(decision.equalsIgnoreCase("Challenge")) {
			trustedDeviceState="UNASSIGNED";
		}else if(decision.equalsIgnoreCase("BLock")) {
			trustedDeviceState="BANNED";
		}else {
			trustedDeviceState="TRUSTED";
		}
		try {
			OkHttpClient client = new OkHttpClient().newBuilder().build();

			//Read device
			Request request1=readDeivceRequest(context,deviceId);
			Response response1 = client.newCall(request1).execute();

			//Call TrustedDevice ApI
			Request request=callTrustedDeviceApi(context);
			
			//if the device is not present
			if(response1.code()>200) {
				//Add the device
				Response response = client.newCall(request).execute();
				if(response.code()==200) {
					context.sharedState.put("addTrusedDeiceiResponse", response);
					context.sharedState.put("addTrusedDeiceiResponseBody", response.body().string());
				}
			}
			//if device is present
			else if(response1.code()==200) {
				JSONObject deviceListObject = new JSONObject(response1.body().string());
				//get device details
				JSONArray deviceList = deviceListObject.getJSONArray("details");
				//if the device list is empty
				if(deviceList.length()==0) {
					//add the device
					Response response = client.newCall(request).execute();
					if(response.code()==200) {
						context.sharedState.put("addTrusedDeiceiResponse", response);
						context.sharedState.put("addTrusedDeiceiResponseBody", response.body().string());
					}
				}
			} 
		} catch (Exception e) {
			logger.error("Unable to get TMX response for session: ");
			throw new NodeProcessException(e);
		}
	}


	private Request callTrustedDeviceApi(TreeContext context) {
		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType,"{\r\n\"clientId\": \""+context.sharedState.get("kountMerchant").asString()+"\",\r\n\"sessionId\": \""+context.sharedState.get("kountSession").asString()+"\",\r\n\"userId\": \""+context.sharedState.get(USERNAME).asString()+"\",\r\n\"trustState\": \""+trustedDeviceState+"\"\r\n}");
		Request request = new Request.Builder()
				.url("https://"+config.domain())
				.method("POST", body)
				.addHeader("Authorization", "Bearer "+context.sharedState.get("API_KEY").toString())
				.addHeader("Content-Type", "application/json")
				.build();
		return request;
	}


	private Request readDeivceRequest(TreeContext context, String deviceId2) {
		Request request1 = new Request.Builder()  
				.url("https://api-qa06.kount.com/trusted-device/devices/"+deviceId+"/clients/"+context.sharedState.get("kountMerchant").asString()+"/users")
				.method("GET", null)
				.addHeader("Authorization", "Bearer "+context.sharedState.get("API_KEY").toString())
				.addHeader("Content-Type", "application/json")
				.build();
		return request1;
	}

}
