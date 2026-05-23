package io.github.yvancywan.anvilcord.example.plugin;

import io.github.yvancywan.anvilcord.discord.command.SlashCommand;

/**
 * Command model contributed by a runtime-only plugin jar through the event bus.
 */
public final class ExampleRuntimeCommand {

    private static final SlashCommand COMMAND = new SlashCommand(
            "example-runtime",
            "Verifies runtime-only AnvilCord plugin command discovery."
    );

    private ExampleRuntimeCommand() {
    }

    /**
     * @return plugin-owned command model.
     */
    public static SlashCommand definition() {
        return COMMAND;
    }
}

