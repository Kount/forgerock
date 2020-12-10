package com.kount.authnode;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.testng.annotations.Test;

/**
 * The Class KountEventsNodeTest.
 */
class KountEventsNodeTest {

	/** The node. */
	KountEventsNode node;

	/** The config. */
	KountEventsNode.Config config;

	/**
	 * Kount events node for user name null.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = NodeProcessException.class)
	void KountEventsNodeForUserNameNull() throws NodeProcessException {
		JsonValue sharedState = json(object(field("username", null)));

		node = new KountEventsNode(config);
		JsonValue transientState = json(object());
		node.process(getContext(sharedState, transientState, emptyList()));
	}

	/**
	 * Kount events node F config domain is empty.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = NodeProcessException.class)
	void KountEventsNodeFConfigDomainIsEmpty() throws NodeProcessException {
		JsonValue sharedState = json(object());
		node = new KountEventsNode(config);
		JsonValue transientState = json(object());
		node.process(getContext(sharedState, transientState, emptyList()));
	}

	/**
	 * Kount events node if Login URL is empty
	 * 
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = NodeProcessException.class)
	void kountEventsNodeIfLoginUrlEmpty() throws NodeProcessException {
		JsonValue sharedState = json(object(field("loginUrl", "")));
		node = new KountEventsNode(config);
		JsonValue transientState = json(object());
		node.process(getContext(sharedState, transientState, emptyList()));
	}

	/**
	 * Kount events node if time stamp is null
	 * 
	 */
	@Test
	void kountEventsNodeIfTimestampNull() {
		node = new KountEventsNode(config);
		String timestamp = node.formatTimeStamp(null);
		assertThat(timestamp).isEqualTo("");
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
