package io.github.yvancywan.anvilcord.core.event;

import module java.base;

/**
 * Published once Spring has completed component scanning and application startup.
 */
public record FrameworkInitializationEvent(
        List<String> slashCommandNames,
        int registeredEventTypeCount,
        Instant occurredAt
) implements BotEvent {

    public FrameworkInitializationEvent {
        slashCommandNames = List.copyOf(slashCommandNames == null ? List.of() : slashCommandNames);
        if (registeredEventTypeCount < 0) {
            throw new IllegalArgumentException("registeredEventTypeCount must not be negative");
        }
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}

