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
 * @author reshma.madan
 */
@Node.Metadata(outcomeProvider = KountDecisionNode.KountDecisionOutcomeProvider.class,
        configClass = KountDecisionNode.Config.class, tags = {
        "risk"})
public class KountDecisionNode implements Node {

    private final Logger logger = LoggerFactory.getLogger(KountDecisionNode.class);
    String decision = "";

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

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("Kount Decision Node started");
        boolean definedCheck = context.sharedState.isDefined("kountLoginResponseBody");
        if (definedCheck) {
            JsonValue loginApiResponseBody = context.sharedState.get("kountLoginResponseBody");
            loginApiResponseBody.getObject();
            try {
                JSONObject json = new JSONObject(loginApiResponseBody.asString());
                decision = json.get("decision").toString();
            } catch (JSONException e) {
                throw new NodeProcessException("Kount Login Response is null");
            }
        } else {
            return Action.goTo("Failure").build();
        }
        if ("Challenge".equalsIgnoreCase(decision)) {
            return Action.goTo("Challenge").build();
        } else if ("Block".equalsIgnoreCase(decision)) {
            return Action.goTo("Failure").build();
        } else if ("Allow".equalsIgnoreCase(decision)) {
            return Action.goTo("Success").build();
        } else {
            logger.debug("Error in Kount Decision Node ");
            return Action.goTo("Failure").build();
        }
    }

    public enum KountDecisionOutcome {
        Success,
        Failure,
        Challenge
    }

    public static class KountDecisionOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            return ImmutableList.of(
                    new Outcome(KountDecisionOutcome.Success.name(), "Success"),
                    new Outcome(KountDecisionOutcome.Failure.name(), "Failure"),
                    new Outcome(KountDecisionOutcome.Challenge.name(), "Challenge"));
        }
    }

}
