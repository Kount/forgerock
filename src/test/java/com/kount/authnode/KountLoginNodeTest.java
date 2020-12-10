package com.kount.authnode;

import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentity;

/**
 * The Class KountLoginNodeTest.
 */
class KountLoginNodeTest {

	/** The node. */
	KountLoginNode node;

	/** The config. */
	@Mock
	private KountLoginNode.Config config;

	/** The identity. */
	@Mock
	private AMIdentity identity;

	/** The kount login node. */
	@Mock
	private KountLoginNode kountLoginNode;

	/**
	 * Setup.
	 *
	 * @throws Exception the exception
	 */
	@BeforeMethod
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(identity.isActive()).willReturn(true);
		given(identity.getAttribute("")).willReturn(null);
		given(config.uniqueIdentifier()).willReturn("");
		given(config.domain()).willReturn("");
		// given(loginAPIPost()).willReturn("");
	}

	/**
	 * Kount login for user name null.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = NodeProcessException.class)
	void kountLoginForUserNameNull() throws NodeProcessException {
		JsonValue sharedState = json(object(field("username", null)));

		node = new KountLoginNode(config);
		JsonValue transientState = json(object());
		node.process(getContext(sharedState, transientState, emptyList()));
	}

	/**
	 * Kount login node config domain is empty.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = NodeProcessException.class)
	void kountLoginNodeConfigDomainIsEmpty() throws NodeProcessException {
		JsonValue sharedState = json(object());
		node = new KountLoginNode(config);
		JsonValue transientState = json(object());
		TreeContext context = getContext(sharedState, transientState, emptyList());
		node.getKountLoginRequest(context, identity);
	}

	/**
	 * Kount login node config unique identifier is empty.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = NodeProcessException.class)
	void kountLoginNodeConfigUnique_IdentifierIsEmpty() throws NodeProcessException {
		JsonValue sharedState = json(object());
		node = new KountLoginNode(config);
		JsonValue transientState = json(object());
		TreeContext context = getContext(sharedState, transientState, emptyList());
		node.getKountLoginRequest(context, identity);
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
