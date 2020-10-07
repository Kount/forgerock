package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.Iterator;
import java.util.Set;

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
	public KountEventsNode(@Assisted Config config, HttpClientHandler client) {
		this.config = config;
	}	
	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		logger.debug("Kount Login Events started");
		context.sharedState.put("API_KEY", config.API_KEY());
		System.out.println("1");
		callEventsAPI(context);
		System.out.println("2");
		System.out.println("Kount Login Events Node completed");
		return goToNext().replaceSharedState(context.sharedState).build();
	}
	
	/**
	 * @param context
	 * @throws NodeProcessException
	 */
	private void callEventsAPI(TreeContext context) throws NodeProcessException {
		System.out.println("-----Inside Kount Login EVENTS callEventsAPI--------------");
		try {
			OkHttpClient client = new OkHttpClient().newBuilder().build();
			Request request=getRequest(context);
					Response response = client.newCall(request).execute();
					if(response.code()==200) {
						System.out.println("response code= "+response.code());
						context.sharedState.put("kountEventsResponseBody", response.body().string());
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
		RequestBody body = RequestBody.create(mediaType,"{\r\n  \"failedAttempt\": {\r\n \"clientId\": \""+context.sharedState.get("kountMerchant").asString()+"\",\r\n\"sessionId\": \""+context.sharedState.get("kountSession").asString()+"\",\r\n\"userId\": \""+mail+"\",\r\n\"username\": \""+context.sharedState.get(USERNAME).asString()+"\",\r\n\"userPassword\": \""+encryptedPwd.run().toString()+"\",\r\n\"userIp\": \""+ip+"\"\r\n}\r\n}");
		Request request = new Request.Builder()
				.url("https://"+config.domain())
				.method("POST", body)
				.addHeader("Authorization","Bearer "+config.API_KEY())
				.addHeader("Content-Type", "application/json")
				.build();
		return request;
	}

}