package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.Action.send;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;

import org.apache.commons.io.IOUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that injects the login page with the Kount Data Collector JavaScript
 * SDK. This code will also create a unique session ID for each session. The
 * Kount Data Collector SDK will send all the collected data to the Kount
 * Control service for each session
 */

@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = KountDataCollectorNode.Config.class, tags = {
		"risk" })
public class KountDataCollectorNode extends SingleOutcomeNode {

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(KountDataCollectorNode.class);

	/** The config. */
	private final Config config;

	/**
	 * Configuration for the node.
	 */
	public interface Config {

		/**
		 * Merchant id.
		 *
		 * @return the string
		 */
		@Attribute(order = 100, requiredValue = true)
		default String merchantId() {
			return Constants.KOUNT_PROFILER_MERCHANT_ID;
		}

		/**
		 * Domain.
		 *
		 * @return the string
		 */
		@Attribute(order = 200, validators = { RequiredValueValidator.class })
		default String domain() {
			return Constants.KOUNT_PROFILER_DOMAIN;
		}

	}

	/**
	 * Instantiates a new kount Data Collector node.
	 *
	 * @param config the config
	 */
	@Inject
	public KountDataCollectorNode(@Assisted Config config) {
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
		logger.info("Kount Data Collector Node started");
		Optional<String> result = context.getCallback(HiddenValueCallback.class).map(HiddenValueCallback::getValue)
				.filter(scriptOutput -> !Strings.isNullOrEmpty(scriptOutput));
		if (result.isPresent()) {
			JsonValue newSharedState = context.sharedState.copy();
			newSharedState.put("kountResult", result.get());
			return goToNext().replaceSharedState(newSharedState).build();
		} else {
			// Generating session ID
			final String sessionID = UUID.randomUUID().toString().replace("-", "");
			StringBuilder callback = new StringBuilder();
			callback.append(dataCollectorJS(sessionID));
			callback.append("\n");
			callback.append(addClassToBodyJS());

			// Putting session and merchantId in context
			context.sharedState.put("kountSession", sessionID);
			context.sharedState.put("kountMerchant", config.merchantId());

			ScriptTextOutputCallback scriptAndSelfSubmitCallback = new ScriptTextOutputCallback(callback.toString());

			HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("kountResult");

			ImmutableList<Callback> callbacks = ImmutableList.of(scriptAndSelfSubmitCallback, hiddenValueCallback);

			return send(callbacks).build();
		}
	}

	/**
	 * Data collector JS.
	 *
	 * @param sessionId the session id
	 * @return the string
	 * @throws NodeProcessException the node process exception
	 */
	private String dataCollectorJS(String sessionId) throws NodeProcessException {
		logger.debug("In KountDataCollectorNode.dataCollectorJS()");
		String dataCollector = getScriptAsString(Constants.DATA_COLLECTOR);

		// find and replace
		dataCollector = dataCollector.replace("sessionId", sessionId);
		dataCollector = dataCollector.replace("merchantId", config.merchantId());
		dataCollector = dataCollector.replace("domain", config.domain());

		return dataCollector;
	}

	/**
	 * Adds the class to body JS.
	 *
	 * @return the string
	 * @throws NodeProcessException the node process exception
	 */
	private String addClassToBodyJS() throws NodeProcessException {
		return getScriptAsString(Constants.BODY_CLASS);
	}

	/**
	 * Gets a JavaScript script as a String.
	 *
	 * @param scriptFileName the filename of the script.
	 * @return the script as an executable string.
	 * @throws NodeProcessException if the file doesn't exist.
	 */

	public String getScriptAsString(String scriptFileName) throws NodeProcessException {
		logger.debug("In KountDataCollectorNode.getScriptAsString()");
		InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(scriptFileName);
		String script;
		try {
			script = IOUtils.toString(resourceStream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.error("ERROR: KountDataCollectorNode.getScriptAsString(), Failed to get the script, Message:"
					+ e.getMessage());
			e.printStackTrace();
			throw new NodeProcessException(e);
		}
		return script;
	}

}
