package io.github.yvancywan.anvilcord.core.event;

import module java.base;

/**
 * Published when a gateway adapter reports a disconnect for any shard/session.
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
