package com.yvan.cywan.anvilcord.example.plugin;

import com.yvan.cywan.anvilcord.core.event.BotEvent;

import java.time.Instant;

/**
 * Plugin-defined event published when the sample plugin entrypoint runs.
 */
public record ExamplePluginInitializedEvent(String pluginId, Instant occurredAt) implements BotEvent {

    public ExamplePluginInitializedEvent {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}

