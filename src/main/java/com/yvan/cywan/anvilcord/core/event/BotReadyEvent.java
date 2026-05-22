package com.yvan.cywan.anvilcord.core.event;
import module java.base;
/**
 * Published after Discord confirms the gateway session is ready.
 */
public record BotReadyEvent(BotUserProfile userProfile, Instant occurredAt) implements BotEvent {
    public BotReadyEvent {
        userProfile = Objects.requireNonNull(userProfile, "userProfile");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
