package io.github.yvancywan.anvilcord.starter;

import io.github.yvancywan.anvilcord.discord.command.SlashCommand;

/**
 * Default health-check command definition.
 */
public final class PingCommand {

    private static final SlashCommand COMMAND = new SlashCommand(
            "ping",
            "Verify that the bot framework command pipeline is alive."
    );

    private PingCommand() {
    }

    /**
     * @return built-in ping command model.
     */
    public static SlashCommand definition() {
        return COMMAND;
    }
}

