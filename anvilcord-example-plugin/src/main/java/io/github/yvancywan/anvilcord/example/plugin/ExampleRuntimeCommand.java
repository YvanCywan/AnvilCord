package io.github.yvancywan.anvilcord.example.plugin;

import io.github.yvancywan.anvilcord.discord.command.SlashCommand;

/**
 * Command model contributed by a runtime-only plugin jar through annotation scanning.
 */
@SlashCommand(
        name = ExampleRuntimeCommand.NAME,
        description = "Verifies runtime-only AnvilCord plugin command discovery."
)
public final class ExampleRuntimeCommand {

    static final String NAME = "example-runtime";

    private ExampleRuntimeCommand() {
    }
}

