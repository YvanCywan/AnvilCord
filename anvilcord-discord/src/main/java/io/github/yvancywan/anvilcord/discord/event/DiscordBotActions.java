package io.github.yvancywan.anvilcord.discord.event;

import io.github.yvancywan.anvilcord.core.event.BotEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Discord bot actions consumed by the Discord gateway bridge.
 */
public final class DiscordBotActions {

    private DiscordBotActions() {
    }

    /** Requests that the bot send a text message to a channel. */
    public record SendChannelMessage(String channelId, String content, String correlationId, Instant occurredAt) implements BotEvent {
        public SendChannelMessage { channelId = requireText(channelId, "channelId"); content = requireText(content, "content"); correlationId = defaultCorrelation(correlationId); occurredAt = defaultNow(occurredAt); }
    }

    /** Requests that the bot send a direct message to a user. */
    public record SendDirectMessage(String userId, String content, String correlationId, Instant occurredAt) implements BotEvent {
        public SendDirectMessage { userId = requireText(userId, "userId"); content = requireText(content, "content"); correlationId = defaultCorrelation(correlationId); occurredAt = defaultNow(occurredAt); }
    }

    /** Requests that the bot edit one of its messages. */
    public record EditMessage(String channelId, String messageId, String content, String correlationId, Instant occurredAt) implements BotEvent {
        public EditMessage { channelId = requireText(channelId, "channelId"); messageId = requireText(messageId, "messageId"); content = requireText(content, "content"); correlationId = defaultCorrelation(correlationId); occurredAt = defaultNow(occurredAt); }
    }

    /** Requests that the bot delete a message. */
    public record DeleteMessage(String channelId, String messageId, String reason, String correlationId, Instant occurredAt) implements BotEvent {
        public DeleteMessage { channelId = requireText(channelId, "channelId"); messageId = requireText(messageId, "messageId"); reason = nullToEmpty(reason); correlationId = defaultCorrelation(correlationId); occurredAt = defaultNow(occurredAt); }
    }

    /** Requests that the bot add a Unicode emoji reaction to a message. */
    public record AddReaction(String channelId, String messageId, String unicodeEmoji, String correlationId, Instant occurredAt) implements BotEvent {
        public AddReaction { channelId = requireText(channelId, "channelId"); messageId = requireText(messageId, "messageId"); unicodeEmoji = requireText(unicodeEmoji, "unicodeEmoji"); correlationId = defaultCorrelation(correlationId); occurredAt = defaultNow(occurredAt); }
    }

    /** Requests that the bot remove its own Unicode emoji reaction from a message. */
    public record RemoveSelfReaction(String channelId, String messageId, String unicodeEmoji, String correlationId, Instant occurredAt) implements BotEvent {
        public RemoveSelfReaction { channelId = requireText(channelId, "channelId"); messageId = requireText(messageId, "messageId"); unicodeEmoji = requireText(unicodeEmoji, "unicodeEmoji"); correlationId = defaultCorrelation(correlationId); occurredAt = defaultNow(occurredAt); }
    }

    /** Requests that the bot emit a typing indicator in a message channel. */
    public record StartTyping(String channelId, String correlationId, Instant occurredAt) implements BotEvent {
        public StartTyping { channelId = requireText(channelId, "channelId"); correlationId = defaultCorrelation(correlationId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published after an action completes successfully. */
    public record ActionSucceeded(String actionType, String correlationId, String resultId, Instant occurredAt) implements BotEvent {
        public ActionSucceeded { actionType = requireText(actionType, "actionType"); correlationId = requireText(correlationId, "correlationId"); resultId = nullToEmpty(resultId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published after an action cannot be completed. */
    public record ActionFailed(String actionType, String correlationId, String errorMessage, Instant occurredAt) implements BotEvent {
        public ActionFailed { actionType = requireText(actionType, "actionType"); correlationId = requireText(correlationId, "correlationId"); errorMessage = requireText(errorMessage, "errorMessage"); occurredAt = defaultNow(occurredAt); }
    }

    private static String defaultCorrelation(String correlationId) {
        return correlationId == null || correlationId.isBlank() ? java.util.UUID.randomUUID().toString() : correlationId;
    }

    private static Instant defaultNow(Instant occurredAt) {
        return occurredAt == null ? Instant.now() : occurredAt;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(name, "name");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

