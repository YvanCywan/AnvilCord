package io.github.yvancywan.anvilcord.starter;

import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;
import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPlugin;
import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPluginContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.Objects;

/**
 * Invokes runtime plugin entrypoints after Spring has created singleton beans and
 * before lifecycle beans, including the Discord gateway bridge, are started.
 */
@Slf4j
public final class AnvilCordPluginInitializer implements SmartInitializingSingleton, ResourceLoaderAware {

    private final VirtualEventBus eventBus;
    private ResourceLoader resourceLoader;

    public AnvilCordPluginInitializer(VirtualEventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<AnvilCordPlugin> plugins = AnvilCordPluginCatalog.plugins(resourceLoader);
        if (plugins.isEmpty()) {
            log.debug("No runtime AnvilCord plugins discovered");
            return;
        }

        AnvilCordPluginContext context = new AnvilCordPluginContext(eventBus);
        for (AnvilCordPlugin plugin : plugins) {
            initializePlugin(plugin, context);
        }
    }

    private static void initializePlugin(AnvilCordPlugin plugin, AnvilCordPluginContext context) {
        try {
            plugin.initialize(context);
            log.info("Initialized AnvilCord plugin '{}'", plugin.id());
        } catch (Exception exception) {
            throw new IllegalStateException("AnvilCord plugin '" + plugin.id() + "' failed to initialize", exception);
        }
    }
}

