package io.github.yvancywan.anvilcord.starter;

import io.github.yvancywan.anvilcord.discord.command.SimpleSlashCommand;
import io.github.yvancywan.anvilcord.discord.command.SlashCommand;
import io.github.yvancywan.anvilcord.discord.command.SlashCommandDefinition;

import java.util.List;
import java.util.Arrays;

/** Factory methods used by generated slash-command wrapper bean definitions. */
public final class SlashCommandDefinitionFactory {

    private SlashCommandDefinitionFactory() {
    }

    /**
     * Creates an immutable slash-command definition from annotation metadata.
     *
     * @param commandClassName fully qualified annotated command class name
     * @return slash-command definition wrapper
     */
    public static SlashCommandDefinition fromAnnotation(String commandClassName) {
        Class<?> commandClass = commandClass(commandClassName);
        SlashCommand slashCommand = commandClass.getAnnotation(SlashCommand.class);
        if (slashCommand == null) {
            throw new IllegalStateException(commandClassName + " is not annotated with @SlashCommand");
        }
        return new SimpleSlashCommand(slashCommand.name(), slashCommand.description(), options(slashCommand));
    }

    private static Class<?> commandClass(String commandClassName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = SlashCommandDefinitionFactory.class.getClassLoader();
            }
            return Class.forName(commandClassName, false, classLoader);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to load @SlashCommand class " + commandClassName, exception);
        }
    }

    private static List<SlashCommandDefinition.Option> options(SlashCommand slashCommand) {
        return Arrays.stream(slashCommand.options())
                .map(option -> new SlashCommandDefinition.Option(
                        option.name(),
                        option.description(),
                        option.type(),
                        option.required()
                ))
                .toList();
    }
}


