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

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.testng.annotations.Test;



class KountDecisionNodeTest {
     
     KountDecisionNode node;
     
     KountDecisionNode.Config config;
     

     @Test
     void kountDecisionForAllow() throws NodeProcessException {
            JsonValue sharedState = json(object(
                     field("kountLoginResponseBody", "{\r\n\t\"decision\":\"Allow\",\r\n}")
             ));
            
            node = new KountDecisionNode(config);
             JsonValue transientState = json(object());
            Action result = node.process(getContext(sharedState, transientState, emptyList()));
            assertThat(result.outcome).isEqualTo("Success");
     }
     
     @Test
     void kountDecisionForFailure() throws NodeProcessException {
            JsonValue sharedState = json(object(
                     field("kountLoginResponseBody", "{\r\n\t\"decision\":\"Failure\",\r\n}")
             ));
            
            node = new KountDecisionNode(config);
             JsonValue transientState = json(object());
            Action result = node.process(getContext(sharedState, transientState, emptyList()));
            assertThat(result.outcome).isEqualTo("Failure");
     }
     
     @Test
     void kountDecisionForNotKnowValueResponse() throws NodeProcessException {
            JsonValue sharedState = json(object(
                     field("kountLoginResponseBody", "{\r\n\t\"decision\":\"Not Known\",\r\n}")
             ));
            
            node = new KountDecisionNode(config);
             JsonValue transientState = json(object());
            Action result = node.process(getContext(sharedState, transientState, emptyList()));
            assertThat(result.outcome).isEqualTo("Failure");
     }
     
     @Test
     void kountDecisionForChallenge() throws NodeProcessException {
            JsonValue sharedState = json(object(
                     field("kountLoginResponseBody", "{\r\n\t\"decision\":\"Challenge\",\r\n}")
             ));
            
            node = new KountDecisionNode(config);
             JsonValue transientState = json(object());
            Action result = node.process(getContext(sharedState, transientState, emptyList()));
            assertThat(result.outcome).isEqualTo("Challenge");
     }
     
     @Test(expectedExceptions = NodeProcessException.class)
     void kountDecisionForLoginResponseNull() throws NodeProcessException {
            JsonValue sharedState = json(object(
                     field("kountLoginResponseBody", "")
             ));
            
            node = new KountDecisionNode(config);
             JsonValue transientState = json(object());
            Action result = node.process(getContext(sharedState, transientState, emptyList()));
            assertThat(result.outcome).isEqualTo("Challenge");
     }
     
     @Test
     void kountDecisionEmptyResponse() throws NodeProcessException {
            JsonValue sharedState = json(object());
            
            node = new KountDecisionNode(config);
             JsonValue transientState = json(object());
            Action result = node.process(getContext(sharedState, transientState, emptyList()));
            assertThat(result.outcome).isEqualTo("Failure");
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
