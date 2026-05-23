package io.github.yvancywan.anvilcord.discord.command;

import java.util.List;

/**
 * Framework-level slash-command definition made only of standard Java values.
 *
 * <p>Plugin modules can publish or contribute this model without compiling
 * against Discord4J. The Discord module adapts it into Discord application
 * command requests and publishes invocation events back to the shared event bus.</p>
 */
public record SlashCommand(String name, String description, List<Option> options) {

    /**
     * Creates a command without options.
     *
     * @param name        Discord command name
     * @param description user-facing command description
     */
    public SlashCommand(String name, String description) {
        this(name, description, List.of());
    }

    /**
     * Normalizes and validates command metadata.
     */
    public SlashCommand {
        name = requireText(name, "name");
        description = requireText(description, "description");
        options = List.copyOf(options == null ? List.of() : options);
    }

    /** Standard Java representation of a slash-command option. */
    public record Option(String name, String description, OptionType type, boolean required) {
        public Option {
            name = requireText(name, "option name");
            description = requireText(description, "option description");
            type = type == null ? OptionType.STRING : type;
        }
    }

    /** Supported option kinds mapped by {@code anvilcord-discord} to Discord option types. */
    public enum OptionType {
        STRING(3),
        INTEGER(4),
        BOOLEAN(5),
        USER(6),
        CHANNEL(7),
        ROLE(8),
        NUMBER(10);

        private final int discordType;

        OptionType(int discordType) {
            this.discordType = discordType;
        }

        /**
         * @return Discord application-command option type integer.
         */
        public int discordType() {
            return discordType;
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slash command " + fieldName + " must not be blank");
        }
        return value;
    }
}
