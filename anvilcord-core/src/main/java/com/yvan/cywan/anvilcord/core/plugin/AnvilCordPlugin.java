package com.yvan.cywan.anvilcord.core.plugin;

import java.util.Set;

/**
 * Runtime-discoverable plugin contract for AnvilCord extension jars.
 *
 * <p>Plugin jars expose implementations with Java's {@link java.util.ServiceLoader}
 * by adding a {@code META-INF/services/com.yvan.cywan.anvilcord.core.plugin.AnvilCordPlugin}
 * resource. Host applications can then place the plugin jar on the runtime
 * classpath, for example with Gradle {@code runtimeOnly}, without compiling
 * against the plugin's classes.</p>
 */
public interface AnvilCordPlugin {

    /**
     * @return stable human-readable plugin identifier used for diagnostics
     */
    String id();

    /**
     * Returns package roots that AnvilCord should scan for plugin-owned Spring
     * components and annotation-free framework contracts such as slash commands.
     *
     * <p>The default scans the implementation class package, including its
     * subpackages.</p>
     *
     * @return package roots to scan when this plugin is present at runtime
     */
    default Set<String> scanBasePackages() {
        Package pluginPackage = getClass().getPackage();
        if (pluginPackage == null || pluginPackage.getName().isBlank()) {
            return Set.of();
        }
        return Set.of(pluginPackage.getName());
    }
}

