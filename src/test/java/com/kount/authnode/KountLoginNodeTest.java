package com.kount.authnode;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentity;




class KountLoginNodeTest {

	KountLoginNode node;

	@Mock
	private KountLoginNode.Config config;

	AMIdentity identity;


	@BeforeMethod
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(config.userType()).willReturn(null);
	}

	@Test(expectedExceptions = NodeProcessException.class)
	void kountLoginForUserNameNull() throws NodeProcessException {
		JsonValue sharedState = json(object(
				field("username", null)
				));

		node = new KountLoginNode(config);
		JsonValue transientState = json(object());
		node.process(getContext(sharedState, transientState, emptyList()));
	}

	@Test(expectedExceptions = NodeProcessException.class)
	void kountLoginNodeConfigUSerTypeEmpty() throws NodeProcessException {
		JsonValue sharedState = json(object(
				field("username", "test")
				));
		node = new KountLoginNode(config);
		JsonValue transientState = json(object());
		TreeContext context=getContext(sharedState, transientState, emptyList());
		node.getKountLoginRequest(context,identity);
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
