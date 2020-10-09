package com.kount.authnode;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.testng.annotations.Test;

class KountTrustedDeviceNodeTest {

	KountTrustedDeviceNode node;

	KountTrustedDeviceNode.Config config;

	   @Test(expectedExceptions = NodeProcessException.class)
	     void KountTrustedDeviceNodeLoginResponseNull() throws NodeProcessException {
	            JsonValue sharedState = json(object(
	                     field("kountLoginResponseBody", "")
	             ));
	            
	            node = new KountTrustedDeviceNode(config);
	             JsonValue transientState = json(object());
	            Action result = node.process(getContext(sharedState, transientState, emptyList()));
	            assertThat(result.outcome).isEqualTo("Challenge");
	     }
	private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
			List<? extends Callback> callbacks) {
		return getContext(sharedState, transientState, callbacks, Optional.of("bob"));
	}

	private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
			List<? extends Callback> callbacks, Optional<String> universalId) {
		return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, universalId);
	}
}
