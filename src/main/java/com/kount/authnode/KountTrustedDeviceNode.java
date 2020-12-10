package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.net.HttpURLConnection;
import java.text.MessageFormat;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.sm.validation.URLValidator;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.kount.authnode.HttpConnection.HTTPResponse;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The KountTrustedDeviceNode is a single-outcome node which should be used
 * when Kount Control responds with Allow (Success) or Challenge.
 * The node is responsible for checking whether the given device is already on the trusted device list,
 * and adding the device to the trusted device list if it’s not
 * @author reshma.madan
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = KountTrustedDeviceNode.Config.class, tags = {
		"risk" })
public class KountTrustedDeviceNode extends SingleOutcomeNode {

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(KountTrustedDeviceNode.class);

	/** The config. */
	private final Config config;

	/** The http connection. */
	private final HttpConnection httpConnection = new HttpConnection();

	/** The trusted device state. */
	private String trustedDeviceState = "";

	/**
	 * Configuration for the node.
	 */
	public interface Config {

		/**
		 * Domain.
		 *
		 * @return the string
		 */
		@Attribute(order = 200, validators = { RequiredValueValidator.class, URLValidator.class })
		default String domain() {
			return Constants.KOUNT_TRUSTED_DEVICE_DOMAIN;
		}
	}

	/**
	 * Guice constructor.
	 *
	 * @param config The config for this instance.
	 */
	@Inject
	public KountTrustedDeviceNode(@Assisted Config config) {
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
		logger.info("Kount Trusted Device Node started");
		boolean definedCheck = context.sharedState.isDefined("kountLoginResponseBody");
		String deviceId;
		String decision;
		if (definedCheck) {
			JsonValue loginApiResponseBody = context.sharedState.get("kountLoginResponseBody");
			loginApiResponseBody.getObject();
			try {
				JSONObject json = new JSONObject(loginApiResponseBody.asString());
				decision = json.get("decision").toString();
				deviceId = json.get("deviceId").toString();
				addTrustedDevice(context, decision, deviceId);
			} catch (JSONException e) {
				logger.error("ERROR: KountTrustedDeviceNode.process(), Message: " + e.getMessage());
				throw new NodeProcessException("Kount Login Response is null");
			}
		} else {
			logger.error("ERROR: KountTrustedDeviceNode.process(), Message: Unable to get kountLoginResponseBody");
			throw new NodeProcessException("Kount Login Response is null");
		}

		return goToNext().replaceSharedState(context.sharedState).build();
	}

	/**
	 * Adds the trusted device.
	 *
	 * @param context  the context
	 * @param decision the decision
	 * @param deviceId the device id
	 * @return the string
	 * @throws NodeProcessException the node process exception
	 */
	String addTrustedDevice(TreeContext context, String decision, String deviceId) throws NodeProcessException {
		logger.debug("In KountTrustedDeviceNode.addTrustedDevice()");
		HttpURLConnection readDeviceConnection = null;

		if (decision != null && !decision.isEmpty()) {
			if ("Challenge".equalsIgnoreCase(decision)) {
				trustedDeviceState = Constants.TRUSTED_DEVICE_STATE_UNASSIGNED;
			} else if ("Block".equalsIgnoreCase(decision)) {
				trustedDeviceState = Constants.TRUSTED_DEVICE_STATE_BANNED;
			} else {
				trustedDeviceState = Constants.TRUSTED_DEVICE_STATE_TRUSTED;
			}
		} else {
			return "Decision is empty";
		}

		try {
			if (!deviceId.contains(Constants.DEVICE_NOT_FOUND)) {
				readDeviceConnection = readDeviceConnection(context, deviceId);
			}

			HttpURLConnection postTrustedDeviceConnection = postTrustedDevice(context);

			// if the device is not present the response code will be 404 or greater
			if (readDeviceConnection == null || readDeviceConnection.getResponseCode() == 404) {
				// Add the device
				HTTPResponse httpResponse = httpConnection.parseResponse(postTrustedDeviceConnection);

				if (postTrustedDeviceConnection.getResponseCode() == 200) {
					logger.info("KountTrustedDeviceNode.addTrustedDevice(): Post Request Success, status code:"
							+ postTrustedDeviceConnection.getResponseCode());
					context.sharedState.put("addTrusedDeiceiResponse", httpResponse.getBuilder());
					context.sharedState.put("addTrusedDeiceiResponseBody", httpResponse.getBuilder().toString());
				}
			}
		} catch (Exception e) {
			logger.error(
					"ERROR: KountTrustedDeviceNode.addTrustedDevice(), Unable to get TMX response for session, Message:"
							+ e.getMessage());
			throw new NodeProcessException(e);
		}
		return decision;
	}

	/**
	 * Post trusted device.
	 *
	 * @param context the context
	 * @return the http URL connection
	 */
	private HttpURLConnection postTrustedDevice(TreeContext context) {
		logger.debug("In KountTrustedDeviceNode.postTrustedDevice()");
		HttpURLConnection connection = null;
		try {

			String payload = getRequestPayload(context);
			connection = httpConnection.post(config.domain(), payload, context.sharedState.get("API_KEY").asString());
		} catch (Exception e) {
			logger.error(
					"ERROR: KountTrustedDeviceNode.postTrustedDevice(), Unable to post Trusted Device API, Message:"
							+ e.getMessage());
			e.printStackTrace();
		}
		return connection;
	}

	/**
	 * Read device connection.
	 *
	 * @param context  the context
	 * @param deviceId the device id
	 * @return the http URL connection
	 */
	private HttpURLConnection readDeviceConnection(TreeContext context, String deviceId) {
		logger.debug("In KountTrustedDeviceNode.readDeviceConnection()");
		HttpURLConnection connection;

		String uri = config.domain().concat(MessageFormat.format(Constants.API_GET_TRUSTED_DEVICE_PATH, deviceId,
				context.sharedState.get("kountMerchant").asString()));

		connection = httpConnection.get(uri, context.sharedState.get("API_KEY").asString());

		return connection;
	}

	/**
	 * Helper method to generate request payload.
	 * 
	 * @param context
	 * @return the payload
	 * @throws JSONException
	 */
	private String getRequestPayload(TreeContext context) throws JSONException {
		logger.debug("In KountTrustedDeviceNode.getRequestPayload()");
		JSONObject payload = new JSONObject();
		payload.put("clientId", context.sharedState.get("kountMerchant").asString());
		payload.put("sessionId", context.sharedState.get("kountSession").asString());
		payload.put("userId", context.sharedState.get(USERNAME).asString());
		payload.put("trustState", trustedDeviceState);
		return payload.toString();
	}
}
