package io.github.yvancywan.anvilcord.starter;

import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Loads AnvilCord runtime plugins from the application classpath.
 */
@Slf4j
final class AnvilCordPluginCatalog {

    private static final ConcurrentMap<ClassLoader, Set<String>> SCAN_BASE_PACKAGES_BY_CLASS_LOADER =
            new ConcurrentHashMap<>();

    private AnvilCordPluginCatalog() {
    }

    static Set<String> scanBasePackages(ResourceLoader resourceLoader) {
        ClassLoader classLoader = pluginClassLoader(resourceLoader);
        return SCAN_BASE_PACKAGES_BY_CLASS_LOADER.computeIfAbsent(classLoader, AnvilCordPluginCatalog::loadScanBasePackages);
    }

    static List<AnvilCordPlugin> plugins(ResourceLoader resourceLoader) {
        return loadPlugins(pluginClassLoader(resourceLoader));
    }

    private static Set<String> loadScanBasePackages(ClassLoader classLoader) {
        Set<String> basePackages = new LinkedHashSet<>();
        for (AnvilCordPlugin plugin : loadPlugins(classLoader)) {
            Set<String> pluginPackages = plugin.scanBasePackages();
            if (pluginPackages == null) {
                log.warn("AnvilCord plugin '{}' returned null scan base packages; ignoring", plugin.id());
                continue;
            }

            for (String basePackage : pluginPackages) {
                if (basePackage != null && !basePackage.isBlank()) {
                    basePackages.add(basePackage);
                }
            }
        }
        return Collections.unmodifiableSet(basePackages);
    }

    private static List<AnvilCordPlugin> loadPlugins(ClassLoader classLoader) {
        ServiceLoader<AnvilCordPlugin> serviceLoader = ServiceLoader.load(AnvilCordPlugin.class, classLoader);
        List<AnvilCordPlugin> plugins = new ArrayList<>();
        try {
            for (AnvilCordPlugin plugin : serviceLoader) {
                plugins.add(plugin);
                log.info("Discovered AnvilCord plugin '{}' from {}", plugin.id(), plugin.getClass().getName());
            }
        } catch (ServiceConfigurationError error) {
            throw new IllegalStateException("Unable to load AnvilCord plugins from the runtime classpath", error);
        }
        return List.copyOf(plugins);
    }

    private static ClassLoader pluginClassLoader(ResourceLoader resourceLoader) {
        if (resourceLoader != null && resourceLoader.getClassLoader() != null) {
            return resourceLoader.getClassLoader();
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader == null ? AnvilCordPlugin.class.getClassLoader() : contextClassLoader;
    }
}

