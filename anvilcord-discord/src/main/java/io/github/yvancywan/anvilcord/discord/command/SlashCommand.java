package io.github.yvancywan.anvilcord.discord.command;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a slash-command definition.
 *
 * <p>The annotated class does not need to be a Spring bean or implement a
 * framework interface. The starter scans host and plugin packages for this
 * annotation and registers a small {@link SlashCommandDefinition} wrapper that
 * the Discord command orchestrator can synchronize with Discord.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlashCommand {

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
    Option[] options() default {};

    /** Slash-command option metadata declared directly on the annotation. */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Option {

        /**
         * @return Discord option name
         */
        String name();

        /**
         * @return user-facing option description
         */
        String description();

        /**
         * @return option kind
         */
        SlashCommandOptionType type() default SlashCommandOptionType.STRING;

        /**
         * @return whether Discord should require the option
         */
        boolean required() default false;
    }
}
