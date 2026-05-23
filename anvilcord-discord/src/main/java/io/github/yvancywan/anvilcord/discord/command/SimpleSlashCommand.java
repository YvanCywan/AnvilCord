package io.github.yvancywan.anvilcord.discord.command;

import java.util.List;
import java.util.Objects;

/**
 * Immutable {@link SlashCommandDefinition} implementation for programmatic and annotation-backed commands.
 */
public final class SimpleSlashCommand implements SlashCommandDefinition {

	private final String name;
	private final String description;
	private final List<SlashCommandDefinition.Option> options;

	/**
	 * Creates a command without options.
	 *
	 * @param name        Discord command name
	 * @param description user-facing command description
	 */
	public SimpleSlashCommand(String name, String description) {
		this(name, description, List.of());
	}

	/**
	 * Normalizes and validates command metadata.
	 */
	public SimpleSlashCommand(String name, String description, List<SlashCommandDefinition.Option> options) {
		this.name = SlashCommandDefinition.requireText(name, "name");
		this.description = SlashCommandDefinition.requireText(description, "description");
		this.options = List.copyOf(options == null ? List.of() : options);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public List<SlashCommandDefinition.Option> options() {
		return options;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SimpleSlashCommand command)) {
			return false;
		}
		return Objects.equals(name, command.name)
				&& Objects.equals(description, command.description)
				&& Objects.equals(options, command.options);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, options);
	}

	@Override
	public String toString() {
		return "SimpleSlashCommand["
				+ "name=" + name
				+ ", description=" + description
				+ ", options=" + options
				+ ']';
	}
}




