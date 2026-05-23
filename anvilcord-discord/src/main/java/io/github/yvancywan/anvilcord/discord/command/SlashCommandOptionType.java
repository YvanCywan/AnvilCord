package io.github.yvancywan.anvilcord.discord.command;

/** Supported option kinds mapped by {@code anvilcord-discord} to Discord option types. */
public enum SlashCommandOptionType {
    STRING(3),
    INTEGER(4),
    BOOLEAN(5),
    USER(6),
    CHANNEL(7),
    ROLE(8),
    NUMBER(10);

    private final int discordType;

    SlashCommandOptionType(int discordType) {
        this.discordType = discordType;
    }

    /**
     * @return Discord application-command option type integer.
     */
    public int discordType() {
        return discordType;
    }
}

