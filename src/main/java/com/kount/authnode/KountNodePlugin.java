package com.kount.authnode;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;

/**
 * The Class KountNodePlugin is single plugin class for Kount Nodes
 */
public class KountNodePlugin extends AbstractNodeAmPlugin {

	/** The current version. */
	private static String currentVersion = "2.0.0";

	/**
	 * Gets the nodes by version.
	 *
	 * @return the nodes by version
	 */
	@Override
	protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
		return Collections.singletonMap("2.0.0", Arrays.asList(KountDecisionNode.class, KountEventsNode.class,
				KountLoginNode.class, KountDataCollectorNode.class, KountTrustedDeviceNode.class));
	}

	/**
	 * Gets the plugin version.
	 *
	 * @return the plugin version
	 */
	@Override
	public String getPluginVersion() {
		return KountNodePlugin.currentVersion;
	}
}
