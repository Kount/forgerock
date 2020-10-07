package com.kount.authnode;

import static org.forgerock.openam.auth.node.api.Action.send;

import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import com.google.common.base.Strings;

@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, 
configClass = KountProfilerNode.Config.class, tags = {
"risk" })
public class KountProfilerNode extends SingleOutcomeNode {

	private final Logger logger = LoggerFactory.getLogger(KountProfilerNode.class);
	private final Config config;	
	private ClientScriptUtilities scriptUtils;



	static final String RESOURCE_LOCATION = "com/kount/authnode/";
	static final String DATA_COLLECTOR = RESOURCE_LOCATION + "dataCollector.js";
	static final String BODY_CLASS = RESOURCE_LOCATION + "bodyClass.js";

	/**
	 * Configuration for the node.
	 */
	public interface Config {

		@Attribute(order = 100,requiredValue = true)
		default String merchantId() {
			// domain
			return "Merchant ID";
		}

		@Attribute(order = 200,requiredValue = true)
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
	public KountProfilerNode(@Assisted Config config, ClientScriptUtilities scriptUtil) {
		this.config = config;
		this.scriptUtils = scriptUtil;
	}

	@Override
	public Action process(TreeContext context) throws NodeProcessException {
		Optional<String> result = context.getCallback(HiddenValueCallback.class).map(HiddenValueCallback::getValue).filter(scriptOutput -> !Strings.isNullOrEmpty(scriptOutput));
		if (result.isPresent()) {
			JsonValue newSharedState = context.sharedState.copy();
			newSharedState.put("kountResult", result.get());
			return goToNext().replaceSharedState(newSharedState).build();
		} else {
			final String sessionID = UUID.randomUUID().toString().replace("-", "");
			logger.debug("Kount Profiler Node started");
			StringBuilder callback =new StringBuilder();
			callback.append(dataCollectorJS(sessionID));
			callback.append("\n");
			callback.append(addClassToBodyJS());
			context.sharedState.put("kountSession", sessionID);
			context.sharedState.put("kountMerchant",config.merchantId());

			ScriptTextOutputCallback scriptAndSelfSubmitCallback =
					new ScriptTextOutputCallback(callback.toString());

			HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("kountResult");

			ImmutableList<Callback> callbacks = ImmutableList.of(scriptAndSelfSubmitCallback, hiddenValueCallback);

			return send(callbacks).build();
		}
	}

	private String dataCollectorJS(String sessionId) throws NodeProcessException {
		String dataCollector = scriptUtils.getScriptAsString(DATA_COLLECTOR);
		dataCollector = dataCollector.replace("sessionId", sessionId);
		dataCollector = dataCollector.replace("merchantId", config.merchantId());
		dataCollector = dataCollector.replace("domain", config.domain());
		//find and replace
		return dataCollector;
	}

	private String addClassToBodyJS() throws NodeProcessException {
		String dataCollector = scriptUtils.getScriptAsString(BODY_CLASS);
		return dataCollector;
	}


}
