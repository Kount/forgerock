package com.kount.authnode;

import java.util.List;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;

/**
 *  * KountDecisionNode node takes input from the Kount Login node and allows you to configure
 *  * the tree based on the three possible outcomes: Allow, Block or Challenge.
 * @author reshma.madan
 *
 */
@Node.Metadata(outcomeProvider = KountDecisionNode.KountDecisionOutcomeProvider.class, configClass = KountDecisionNode.Config.class, tags = {
		"risk" })
public class KountDecisionNode implements Node {

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(KountDecisionNode.class);

	/**
	 * Configuration for the node.
	 */
	public interface Config {

	}

	/**
	 * Guice constructor.
	 *
	 * @param config The config for this instance.
	 */
	@Inject
	public KountDecisionNode(@Assisted Config config) {
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
		logger.info("Kount Decision Node started");
		boolean definedCheck = context.sharedState.isDefined("kountLoginResponseBody");
		String decision;
		if (definedCheck) {
			JsonValue loginApiResponseBody = context.sharedState.get("kountLoginResponseBody");
			loginApiResponseBody.getObject();
			try {
				JSONObject json = new JSONObject(loginApiResponseBody.asString());
				decision = json.get("decision").toString();
			} catch (JSONException e) {
				logger.error("ERROR: KountDecisionNode.process(), Message: Unable to get Login decision");
				throw new NodeProcessException("Kount Login Response is null");
			}
		} else {
			logger.error("ERROR: KountDecisionNode.process(), Message: Unable to get kountLoginResponseBody");
			return Action.goTo(Constants.DECISION_OUTCOME_FAILURE).build();
		}

		logger.debug("KountDecisionNode.process(), LoginDecision:" + decision);
		if ("Challenge".equalsIgnoreCase(decision)) {
			return Action.goTo(Constants.DECISION_OUTCOME_CHALLENGE).build();
		} else if ("Block".equalsIgnoreCase(decision)) {
			return Action.goTo(Constants.DECISION_OUTCOME_FAILURE).build();
		} else if ("Allow".equalsIgnoreCase(decision)) {
			return Action.goTo(Constants.DECISION_OUTCOME_SUCCESS).build();
		} else {
			logger.error("ERROR: KountDecisionNode.process(), Message: LoginDecision does not matched");
			return Action.goTo(Constants.DECISION_OUTCOME_FAILURE).build();
		}
	}

	/**
	 * The Enum KountDecisionOutcome.
	 */
	private enum KountDecisionOutcome {
		Success, Failure, Challenge
	}

	/**
	 * The Class KountDecisionOutcomeProvider.
	 */
	public static class KountDecisionOutcomeProvider implements OutcomeProvider {

		/**
		 * Gets the outcomes.
		 *
		 * @param locales        the locales
		 * @param nodeAttributes the node attributes
		 * @return the outcomes
		 */
		@Override
		public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
			return ImmutableList.of(
					new Outcome(KountDecisionOutcome.Success.name(), Constants.DECISION_OUTCOME_SUCCESS),
					new Outcome(KountDecisionOutcome.Failure.name(), Constants.DECISION_OUTCOME_FAILURE),
					new Outcome(KountDecisionOutcome.Challenge.name(), Constants.DECISION_OUTCOME_CHALLENGE));
		}
	}

}
