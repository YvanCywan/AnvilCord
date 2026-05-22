package io.github.yvancywan.anvilcord.consumer;

import io.github.yvancywan.anvilcord.core.AnvilCordPluginHost;

/**
 * Example host application that compiles against AnvilCord core contracts while
 * loading the full framework from {@code anvilcord-starter} at runtime.
 *
 * <p>Future plugin modules can be placed under this package, or the scan base
 * can be expanded by a real host application to include external plugin
 * packages.</p>
 */
@AnvilCordPluginHost
public class AnvilCordConsumerApplication {
}

