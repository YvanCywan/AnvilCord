package io.github.yvancywan.anvilcord.example.plugin;

import io.github.yvancywan.anvilcord.core.event.BotReadyEvent;
import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPlugin;
import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPluginContext;
import io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvent;

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
        context.registerListener(DiscordGatewayEvent.class, event -> context.publish(new ExamplePluginObservedDiscordEvent(
                id(),
                event.discordEvent().getClass().getName(),
                Instant.now()
        )));
    }
}


