package com.yvan.cywan.anvilcord.example.plugin;

import com.yvan.cywan.anvilcord.discord.command.SlashCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Annotation-free command contributed by a runtime-only plugin jar.
 */
@SuppressWarnings("unused")
public final class ExampleRuntimeCommand implements SlashCommand {

    private static final ApplicationCommandRequest REQUEST = ApplicationCommandRequest.builder()
            .name("example-runtime")
            .description("Verifies runtime-only AnvilCord plugin command discovery.")
            .build();

    @Override
    public ApplicationCommandRequest commandRequest() {
        return REQUEST;
    }

    @Override
    public void execute(ChatInputInteractionEvent event) {
        event.reply("example runtime plugin").block();
    }
}

