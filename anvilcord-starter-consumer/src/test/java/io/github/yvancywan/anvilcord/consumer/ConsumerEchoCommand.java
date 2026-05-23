package io.github.yvancywan.anvilcord.consumer;

import io.github.yvancywan.anvilcord.discord.command.SlashCommand;

/**
 * Test command model contributed by the consumer application.
 */
final class ConsumerEchoCommand {

    private static final SlashCommand COMMAND = new SlashCommand(
            "consumer-echo",
            "Verifies consumer command model discovery."
    );

    private ConsumerEchoCommand() {
    }

    static SlashCommand definition() {
        return COMMAND;
    }
}


