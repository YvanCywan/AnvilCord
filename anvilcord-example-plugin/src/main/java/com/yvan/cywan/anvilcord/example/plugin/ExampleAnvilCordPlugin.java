package com.yvan.cywan.anvilcord.example.plugin;

import com.yvan.cywan.anvilcord.core.event.BotReadyEvent;
import com.yvan.cywan.anvilcord.core.plugin.AnvilCordPlugin;
import com.yvan.cywan.anvilcord.core.plugin.AnvilCordPluginContext;

import java.time.Instant;

/**
 * Sample plugin descriptor discovered through Java's ServiceLoader.
 */
public final class ExampleAnvilCordPlugin implements AnvilCordPlugin {

    @Override
    public String id() {
        return "example-plugin";
    }

    @Override
    public void initialize(AnvilCordPluginContext context) {
        context.publish(new ExamplePluginInitializedEvent(id(), Instant.now()));
        context.registerListener(BotReadyEvent.class, event -> context.publish(new ExamplePluginObservedBotReadyEvent(
                id(),
                event.userProfile().username(),
                Instant.now()
        )));
    }
}


