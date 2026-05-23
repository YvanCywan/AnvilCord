package io.github.yvancywan.anvilcord.starter;

import io.github.yvancywan.anvilcord.discord.command.SlashCommand;

/**
 * Default health-check command definition.
 */
@SlashCommand(
        name = PingCommand.NAME,
        description = "Verify that the bot framework command pipeline is alive."
)
public final class PingCommand {

    public static final String NAME = "ping";

    private PingCommand() {
    }
}

