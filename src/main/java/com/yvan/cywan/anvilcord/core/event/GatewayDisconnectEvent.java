package com.yvan.cywan.anvilcord.core.event;

import module java.base;

/**
 * Published when Discord4J reports a gateway disconnect for any shard.
 */
public record GatewayDisconnectEvent(
        int statusCode,
        String reason,
        String errorMessage,
        String shard,
        Instant occurredAt
) implements BotEvent {

    public GatewayDisconnectEvent {
        reason = reason == null ? "" : reason;
        errorMessage = errorMessage == null ? "" : errorMessage;
        shard = shard == null ? "unknown" : shard;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
