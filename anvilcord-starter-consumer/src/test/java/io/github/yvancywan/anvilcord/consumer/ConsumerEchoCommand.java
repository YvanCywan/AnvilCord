package io.github.yvancywan.anvilcord.consumer;

import io.github.yvancywan.anvilcord.discord.command.SlashCommand;

/**
 * Test command model contributed by the consumer application through annotation scanning.
 */
@SlashCommand(
        name = "consumer-echo",
        description = "Verifies consumer command model discovery."
)
final class ConsumerEchoCommand {

    private ConsumerEchoCommand() {
    }
}


