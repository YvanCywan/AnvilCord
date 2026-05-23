package io.github.yvancywan.anvilcord.discord.command;

import module java.base;
import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;
import io.github.yvancywan.anvilcord.discord.config.BotCoreProperties;
import io.github.yvancywan.anvilcord.discord.event.DiscordBotActions;
import io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvent;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class SlashCommandOrchestratorTests {

    @Test
    void discoversCommandsAndSkipsDiscordSyncWhenTokenIsBlank() {
        VirtualEventBus eventBus = new VirtualEventBus();
        SlashCommandOrchestrator orchestrator = new SlashCommandOrchestrator(
                List.of(new SlashCommand("alpha", "Test command alpha"), new SlashCommand("beta", "Test command beta")),
                new BotCoreProperties("", ""),
                eventBus
        );

        try {
            assertThat(orchestrator.commandNames()).containsExactly("alpha", "beta");
            assertThatCode(orchestrator::run).doesNotThrowAnyException();
        } finally {
            orchestrator.close();
            eventBus.close();
        }
    }

    @Test
    void rejectsDuplicateCommandNamesAtStartup() {
        VirtualEventBus eventBus = new VirtualEventBus();

        try {
            assertThatThrownBy(() -> new SlashCommandOrchestrator(
                    of(new SlashCommand("duplicate", "first"), new SlashCommand("duplicate", "second")),
                    new BotCoreProperties("", ""),
                    eventBus
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate slash-command name 'duplicate'");
        } finally {
            eventBus.close();
        }
    }

    @Test
    void publishesChatInputInvocationsFromSharedEventBus() throws InterruptedException {
        VirtualEventBus eventBus = new VirtualEventBus();
        CountDownLatch invoked = new CountDownLatch(1);
        SlashCommandOrchestrator orchestrator = new SlashCommandOrchestrator(
                List.of(new SlashCommand("alpha", "Test command alpha")),
                new BotCoreProperties("", ""),
                eventBus
        );
        eventBus.registerListener(SlashCommandInvocationEvent.class, event -> invoked.countDown());

        try {
            ChatInputInteractionEvent interactionEvent = testInteraction("alpha", "999");
            when(interactionEvent.getCommandName()).thenReturn("alpha");

            eventBus.publish(new DiscordGatewayEvent(interactionEvent, Instant.now()));

            assertThat(invoked.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            orchestrator.close();
            eventBus.close();
        }
    }

    @Test
    void registersSlashCommandsPublishedByPlugins() {
        VirtualEventBus eventBus = new VirtualEventBus();
        SlashCommandOrchestrator orchestrator = new SlashCommandOrchestrator(
                List.of(),
                new BotCoreProperties("", ""),
                eventBus
        );

        try {
            eventBus.publish(new SlashCommandRegistrationEvent(new SlashCommand("plugin", "Plugin command"), Instant.now()));

            assertThat(orchestrator.commandNames()).containsExactly("plugin");
        } finally {
            orchestrator.close();
            eventBus.close();
        }
    }

    @Test
    void respondsToPendingInteractionsThroughBotActionEvents() throws InterruptedException {
        VirtualEventBus eventBus = new VirtualEventBus();
        CountDownLatch invoked = new CountDownLatch(1);
        CountDownLatch succeeded = new CountDownLatch(1);
        SlashCommandOrchestrator orchestrator = new SlashCommandOrchestrator(
                List.of(new SlashCommand("alpha", "Test command alpha")),
                new BotCoreProperties("", ""),
                eventBus
        );
        eventBus.registerListener(SlashCommandInvocationEvent.class, event -> invoked.countDown());
        eventBus.registerListener(DiscordBotActions.ActionSucceeded.class, event -> succeeded.countDown());

        try {
            ChatInputInteractionEvent interactionEvent = testInteraction("alpha", "999");
            when(interactionEvent.getCommandName()).thenReturn("alpha");
            InteractionApplicationCommandCallbackReplyMono replyMono = mock(InteractionApplicationCommandCallbackReplyMono.class);
            when(interactionEvent.reply("hello")).thenReturn(replyMono);

            eventBus.publish(new DiscordGatewayEvent(interactionEvent, Instant.now()));
            assertThat(invoked.await(5, TimeUnit.SECONDS)).isTrue();

            eventBus.publish(new DiscordBotActions.RespondToInteraction("999", "hello", "corr", Instant.now()));

            assertThat(succeeded.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            orchestrator.close();
            eventBus.close();
        }
    }

    private static ChatInputInteractionEvent testInteraction(String commandName, String interactionId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(Snowflake.of(123));
        Interaction interaction = mock(Interaction.class);
        when(interaction.getId()).thenReturn(Snowflake.of(interactionId));
        when(interaction.getChannelId()).thenReturn(Snowflake.of(456));
        when(interaction.getGuildId()).thenReturn(Optional.of(Snowflake.of(789)));
        when(interaction.getUser()).thenReturn(user);

        ChatInputInteractionEvent interactionEvent = mock(ChatInputInteractionEvent.class);
        when(interactionEvent.getCommandName()).thenReturn(commandName);
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interactionEvent.getOptions()).thenReturn(List.of());
        return interactionEvent;
    }
}


