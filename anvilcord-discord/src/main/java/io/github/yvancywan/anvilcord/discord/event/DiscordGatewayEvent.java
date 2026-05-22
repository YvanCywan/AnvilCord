package io.github.yvancywan.anvilcord.discord.event;

import io.github.yvancywan.anvilcord.core.event.BotEvent;
import discord4j.core.event.domain.Event;

import java.time.Instant;
import java.util.Objects;

/**
 * Raw Discord4J gateway event published through AnvilCord's shared event bus.
 *
 * <p>Plugins that compile against {@code anvilcord-discord} can listen for this
 * event and filter {@link #discordEvent()} by the Discord4J event subtype they
 * care about.</p>
 */
public record DiscordGatewayEvent(Event discordEvent, Instant occurredAt) implements BotEvent {

    public DiscordGatewayEvent {
        Objects.requireNonNull(discordEvent, "discordEvent");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}

