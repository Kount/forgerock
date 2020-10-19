package com.kount.authnode;

import java.util.Collections;
import java.util.Map;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;

public class KountProfilerNodePlugin extends AbstractNodeAmPlugin {

	private static String currentVersion = "1.0.0";

	//TODO there should only be 1 plugin class. Instead of Collections.singletonList, use Arrays.asList(Node1.class, Node2.class...) in the getNodesByVersion method
	@Override
	  protected Map<String, Iterable<? extends Class<? extends Node>>>
	  getNodesByVersion() {
	    return Collections.singletonMap("1.0.0", Collections.singletonList(KountProfilerNode.class)); 
	  }

	  @Override
	  public String getPluginVersion() {
	    return KountProfilerNodePlugin.currentVersion;
	  }

}
