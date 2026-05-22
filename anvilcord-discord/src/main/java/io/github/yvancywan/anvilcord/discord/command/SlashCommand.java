package io.github.yvancywan.anvilcord.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Imperative slash-command contract implemented by framework and plugin modules.
 *
 * <p>Implementations are ordinary Spring beans. They can perform blocking work
 * inside {@link #execute(ChatInputInteractionEvent)} because the orchestrator
 * invokes each command on a fresh virtual thread.</p>
 */
public interface SlashCommand {

    /**
     * @return Discord application-command metadata used during command sync.
     */
    ApplicationCommandRequest commandRequest();

    /**
     * Executes the command synchronously on a virtual thread.
     *
     * @param event raw Discord4J chat-input interaction event
     * @throws Exception lets module authors use native checked exceptions
     */
    void execute(ChatInputInteractionEvent event) throws Exception;
}

