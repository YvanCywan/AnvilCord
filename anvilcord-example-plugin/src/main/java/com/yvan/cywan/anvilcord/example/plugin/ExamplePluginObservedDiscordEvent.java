package com.yvan.cywan.anvilcord.example.plugin;

import com.yvan.cywan.anvilcord.core.event.BotEvent;

import java.time.Instant;

/**
 * Plugin-defined event published after the sample plugin observes a raw Discord event.
 */
public record ExamplePluginObservedDiscordEvent(
        String pluginId,
        String discordEventType,
        Instant occurredAt
) implements BotEvent {

    public ExamplePluginObservedDiscordEvent {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        if (discordEventType == null || discordEventType.isBlank()) {
            throw new IllegalArgumentException("discordEventType must not be blank");
        }
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}

