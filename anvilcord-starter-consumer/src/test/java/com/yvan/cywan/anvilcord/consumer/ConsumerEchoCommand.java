package com.yvan.cywan.anvilcord.consumer;

import com.yvan.cywan.anvilcord.discord.command.SlashCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Test plugin command intentionally left annotation-free to prove AnvilCord
 * discovers command implementations by contract.
 */
@SuppressWarnings("unused")
final class ConsumerEchoCommand implements SlashCommand {

    private static final ApplicationCommandRequest REQUEST = ApplicationCommandRequest.builder()
            .name("consumer-echo")
            .description("Verifies annotation-free consumer command discovery.")
            .build();

    @Override
    public ApplicationCommandRequest commandRequest() {
        return REQUEST;
    }

    @Override
    public void execute(ChatInputInteractionEvent event) {
        event.reply("echo").block();
    }
}


