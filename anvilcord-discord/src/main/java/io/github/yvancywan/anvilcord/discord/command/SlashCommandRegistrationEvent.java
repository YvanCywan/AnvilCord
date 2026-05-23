package io.github.yvancywan.anvilcord.discord.command;

import io.github.yvancywan.anvilcord.core.event.BotEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Published by plugins that want to contribute a slash command through the event bus.
 */
//noinspection unused
@SuppressWarnings({"unused", "UnusedDeclaration"})
public record SlashCommandRegistrationEvent(SlashCommandDefinition command, Instant occurredAt) implements BotEvent {

    public SlashCommandRegistrationEvent {
        Objects.requireNonNull(command, "command");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}

