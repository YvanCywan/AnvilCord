package io.github.yvancywan.anvilcord.core.event;
import module java.base;
/**
 * Published after the gateway adapter confirms the bot session is ready.
 */
public record BotReadyEvent(BotUserProfile userProfile, Instant occurredAt) implements BotEvent {
    public BotReadyEvent {
        Objects.requireNonNull(userProfile, "userProfile");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
