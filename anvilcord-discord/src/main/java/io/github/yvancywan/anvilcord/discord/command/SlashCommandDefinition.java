package io.github.yvancywan.anvilcord.discord.command;

import java.util.List;

/**
 * Framework-level slash-command definition made only of standard Java values.
 *
 * <p>Plugin modules can publish or contribute this model without compiling
 * against Discord4J. The Discord module adapts it into Discord application
 * command requests and publishes invocation events back to the shared event bus.</p>
 */
public interface SlashCommandDefinition {

    /**
     * @return Discord command name
     */
    String name();

    /**
     * @return user-facing command description
     */
    String description();

    /**
     * @return command options in Discord display order
     */
    List<Option> options();

    /** Standard Java representation of a slash-command option. */
    record Option(String name, String description, SlashCommandOptionType type, boolean required) {
        public Option {
            name = requireText(name, "option name");
            description = requireText(description, "option description");
            type = type == null ? SlashCommandOptionType.STRING : type;
        }
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slash command " + fieldName + " must not be blank");
        }
        return value;
    }
}

