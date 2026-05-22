package com.yvan.cywan.anvilcord.example.plugin;

import com.yvan.cywan.anvilcord.core.event.BotEvent;

import java.time.Instant;

/**
 * Plugin-defined event published after the sample plugin observes BotReadyEvent.
 */
public record ExamplePluginObservedBotReadyEvent(
        String pluginId,
        String botUsername,
        Instant occurredAt
) implements BotEvent {

    public ExamplePluginObservedBotReadyEvent {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        botUsername = botUsername == null ? "" : botUsername;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}

