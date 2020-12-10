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
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.testng.annotations.Test;

/**
 * The Class KountDecisionNodeTest.
 */
class KountDecisionNodeTest {

	/** The node. */
	KountDecisionNode node;

	/** The config. */
	KountDecisionNode.Config config;

	/**
	 * Kount decision for allow.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test
	void kountDecisionForAllow() throws NodeProcessException {
		JsonValue sharedState = json(object(field("kountLoginResponseBody", "{\r\n\t\"decision\":\"Allow\",\r\n}")));

		node = new KountDecisionNode(config);
		JsonValue transientState = json(object());
		Action result = node.process(getContext(sharedState, transientState, emptyList()));
		assertThat(result.outcome).isEqualTo("Success");
	}

	/**
	 * Kount decision for failure.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test
	void kountDecisionForFailure() throws NodeProcessException {
		JsonValue sharedState = json(object(field("kountLoginResponseBody", "{\r\n\t\"decision\":\"Failure\",\r\n}")));

		node = new KountDecisionNode(config);
		JsonValue transientState = json(object());
		Action result = node.process(getContext(sharedState, transientState, emptyList()));
		assertThat(result.outcome).isEqualTo("Failure");
	}

	/**
	 * Kount decision for not know value response.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test
	void kountDecisionForNotKnowValueResponse() throws NodeProcessException {
		JsonValue sharedState = json(
				object(field("kountLoginResponseBody", "{\r\n\t\"decision\":\"Not Known\",\r\n}")));

		node = new KountDecisionNode(config);
		JsonValue transientState = json(object());
		Action result = node.process(getContext(sharedState, transientState, emptyList()));
		assertThat(result.outcome).isEqualTo("Failure");
	}

	/**
	 * Kount decision for challenge.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test
	void kountDecisionForChallenge() throws NodeProcessException {
		JsonValue sharedState = json(
				object(field("kountLoginResponseBody", "{\r\n\t\"decision\":\"Challenge\",\r\n}")));

		node = new KountDecisionNode(config);
		JsonValue transientState = json(object());
		Action result = node.process(getContext(sharedState, transientState, emptyList()));
		assertThat(result.outcome).isEqualTo("Challenge");
	}

	/**
	 * Kount decision for login response null.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test(expectedExceptions = NodeProcessException.class)
	void kountDecisionForLoginResponseNull() throws NodeProcessException {
		JsonValue sharedState = json(object(field("kountLoginResponseBody", "")));

		node = new KountDecisionNode(config);
		JsonValue transientState = json(object());
		Action result = node.process(getContext(sharedState, transientState, emptyList()));
		assertThat(result.outcome).isEqualTo("Challenge");
	}

	/**
	 * Kount decision empty response.
	 *
	 * @throws NodeProcessException the node process exception
	 */
	@Test
	void kountDecisionEmptyResponse() throws NodeProcessException {
		JsonValue sharedState = json(object());

		node = new KountDecisionNode(config);
		JsonValue transientState = json(object());
		Action result = node.process(getContext(sharedState, transientState, emptyList()));
		assertThat(result.outcome).isEqualTo("Failure");
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
