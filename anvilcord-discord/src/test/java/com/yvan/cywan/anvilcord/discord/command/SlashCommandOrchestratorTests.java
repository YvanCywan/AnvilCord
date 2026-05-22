package com.yvan.cywan.anvilcord.discord.command;

import module java.base;
import com.yvan.cywan.anvilcord.core.event.VirtualEventBus;
import com.yvan.cywan.anvilcord.discord.config.BotCoreProperties;
import com.yvan.cywan.anvilcord.discord.event.DiscordGatewayEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
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
                List.of(new TestSlashCommand("alpha"), new TestSlashCommand("beta")),
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
                    of(new TestSlashCommand("duplicate"), new TestSlashCommand("duplicate")),
                    new BotCoreProperties("", ""),
                    eventBus
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate slash-command name 'duplicate'");
        } finally {
            eventBus.close();
        }
    }

    @Test
    void dispatchesChatInputInteractionsFromSharedEventBus() throws InterruptedException {
        VirtualEventBus eventBus = new VirtualEventBus();
        CountDownLatch executed = new CountDownLatch(1);
        SlashCommandOrchestrator orchestrator = new SlashCommandOrchestrator(
                List.of(new TestSlashCommand("alpha", executed)),
                new BotCoreProperties("", ""),
                eventBus
        );

        try {
            ChatInputInteractionEvent interactionEvent = mock(ChatInputInteractionEvent.class);
            when(interactionEvent.getCommandName()).thenReturn("alpha");

            eventBus.publish(new DiscordGatewayEvent(interactionEvent, Instant.now()));

            assertThat(executed.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            orchestrator.close();
            eventBus.close();
        }
    }

    private record TestSlashCommand(String name, CountDownLatch executed) implements SlashCommand {

        private TestSlashCommand(String name) {
            this(name, null);
        }

        @Override
        public ApplicationCommandRequest commandRequest() {
            return ApplicationCommandRequest.builder()
                    .name(name)
                    .description("Test command " + name)
                    .build();
        }

        @Override
        public void execute(ChatInputInteractionEvent event) {
            if (executed == null) {
                throw new UnsupportedOperationException("No live Discord event is needed for this unit test");
            }
            executed.countDown();
        }
    }
}


