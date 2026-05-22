package com.yvan.cywan.anvilcord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.stereotype.Component;

/**
 * Default health-check command proving the full gateway -> virtual thread ->
 * command execution -> Discord response path works end to end.
 */
@Component
public final class PingCommand implements SlashCommand {

    private static final ApplicationCommandRequest REQUEST = ApplicationCommandRequest.builder()
            .name("ping")
            .description("Verify that the bot framework command pipeline is alive.")
            .build();

    @Override
    public ApplicationCommandRequest commandRequest() {
        return REQUEST;
    }

    @Override
    public void execute(ChatInputInteractionEvent event) {
        event.reply("Pong!").block();
    }
}

