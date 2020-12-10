package com.kount.authnode;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The Class KountTrustedDeviceNodeTest.
 */
class KountTrustedDeviceNodeTest {

	/** The node. */
	KountTrustedDeviceNode node;

	/** The config. */
	@Mock
	private KountTrustedDeviceNode.Config config;

	/**
	 * Setup.
	 *
	 * @throws Exception the exception
	 */
	@BeforeMethod
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(config.domain()).willReturn("");
	}

	/**
	 * Kount trusted device node login response null.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = NodeProcessException.class)
	void KountTrustedDeviceNodeLoginResponseNull() throws NodeProcessException {
		JsonValue sharedState = json(object(field("kountLoginResponseBody", "")));

		node = new KountTrustedDeviceNode(config);
		JsonValue transientState = json(object());
		Action result = node.process(getContext(sharedState, transientState, emptyList()));
		assertThat(result.outcome).isEqualTo("Challenge");
	}

	/**
	 * Adds the trusted device test decision is empty.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = { NodeProcessException.class, MalformedURLException.class })
	void addTrustedDeviceTestDecisionIsEmpty() throws NodeProcessException {
		JsonValue sharedState = json(object(field("kountLoginResponseBody", "")));

		node = new KountTrustedDeviceNode(config);
		JsonValue transientState = json(object());
		String str = node.addTrustedDevice(getContext(sharedState, transientState, emptyList()), "Allow", "test");
		assertThat(str).isEqualTo("Decision is empty");
	}

	/**
	 * Gets the context.
	 *
	 * @param sharedState    the shared state
	 * @param transientState the transient state
	 * @param callbacks      the callbacks
	 * @return the context
	 */
	private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
			List<? extends Callback> callbacks) {
		return getContext(sharedState, transientState, callbacks, Optional.of("bob"));
	}

	/**
	 * Gets the context.
	 *
	 * @param sharedState    the shared state
	 * @param transientState the transient state
	 * @param callbacks      the callbacks
	 * @param universalId    the universal id
	 * @return the context
	 */
	private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<? extends Callback> callbacks,
			Optional<String> universalId) {
		return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, universalId);
	}
}
