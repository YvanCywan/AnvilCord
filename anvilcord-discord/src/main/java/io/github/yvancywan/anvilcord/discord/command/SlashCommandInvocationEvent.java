package io.github.yvancywan.anvilcord.discord.command;

import io.github.yvancywan.anvilcord.core.event.BotEvent;

import java.time.Instant;
import java.util.Map;

/**
 * Published when a known Discord chat-input slash command is invoked.
 *
 * <p>Handlers should reply by publishing {@code DiscordBotActions.RespondToInteraction}
 * with the supplied {@link #interactionId()}.</p>
 */
@SuppressWarnings({"unused", "UnusedDeclaration"})
public record SlashCommandInvocationEvent(
        String commandName,
        String interactionId,
        String channelId,
        String guildId,
        String userId,
        Map<String, String> options,
        Instant occurredAt
) implements BotEvent {

    public SlashCommandInvocationEvent {
        commandName = requireText(commandName, "commandName");
        interactionId = requireText(interactionId, "interactionId");
        channelId = nullToEmpty(channelId);
        guildId = nullToEmpty(guildId);
        userId = nullToEmpty(userId);
        options = Map.copyOf(options == null ? Map.of() : options);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

