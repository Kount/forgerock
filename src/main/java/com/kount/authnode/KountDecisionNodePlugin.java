package com.kount.authnode;

import java.util.Collections;
import java.util.Map;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;

/**
 * @author reshma.madan
 *
 */
public class KountDecisionNodePlugin extends AbstractNodeAmPlugin { 

	  private static String currentVersion = "1.0.0"; 

	  @Override
	  protected Map<String, Iterable<? extends Class<? extends Node>>>
	  getNodesByVersion() {
	    return Collections.singletonMap("1.0.0", Collections.singletonList(KountDecisionNode.class)); 
	  }

	  @Override
	  public String getPluginVersion() {
	    return KountDecisionNodePlugin.currentVersion;
	  }
}
