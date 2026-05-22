package com.yvan.cywan.anvilcord.command;

import module java.base;
import com.yvan.cywan.anvilcord.core.config.BotCoreProperties;
import com.yvan.cywan.anvilcord.core.event.VirtualEventBus;
import com.yvan.cywan.anvilcord.discord.DiscordGatewayBridge;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.*;

final class SlashCommandOrchestratorTests {

    @Test
    void discoversCommandsAndSkipsDiscordSyncWhenTokenIsBlank() {
        VirtualEventBus eventBus = new VirtualEventBus();
        DiscordGatewayBridge gatewayBridge = new DiscordGatewayBridge(new BotCoreProperties("", ""), eventBus);
        SlashCommandOrchestrator orchestrator = new SlashCommandOrchestrator(
                List.of(new TestSlashCommand("alpha"), new TestSlashCommand("beta")),
                new BotCoreProperties("", ""),
                gatewayBridge
        );

        try {
            assertThat(orchestrator.commandNames()).containsExactly("alpha", "beta");
            assertThatCode(orchestrator::run).doesNotThrowAnyException();
        } finally {
            orchestrator.close();
            gatewayBridge.stop();
            eventBus.close();
        }
    }

    @Test
    void rejectsDuplicateCommandNamesAtStartup() {
        VirtualEventBus eventBus = new VirtualEventBus();
        DiscordGatewayBridge gatewayBridge = new DiscordGatewayBridge(new BotCoreProperties("", ""), eventBus);

        try {
            assertThatThrownBy(() -> new SlashCommandOrchestrator(
                    of(new TestSlashCommand("duplicate"), new TestSlashCommand("duplicate")),
                    new BotCoreProperties("", ""),
                    gatewayBridge
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate slash-command name 'duplicate'");
        } finally {
            gatewayBridge.stop();
            eventBus.close();
        }
    }

    private record TestSlashCommand(String name) implements SlashCommand {

        @Override
        public ApplicationCommandRequest commandRequest() {
            return ApplicationCommandRequest.builder()
                    .name(name)
                    .description("Test command " + name)
                    .build();
        }

        @Override
        public void execute(ChatInputInteractionEvent event) {
            throw new UnsupportedOperationException("No live Discord event is needed for this unit test");
        }
    }
}


