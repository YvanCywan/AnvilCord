package com.yvan.cywan.anvilcord.consumer;

import module java.base;

import com.yvan.cywan.anvilcord.core.event.BotEvent;
import com.yvan.cywan.anvilcord.core.event.BotReadyEvent;
import com.yvan.cywan.anvilcord.core.event.BotUserProfile;
import com.yvan.cywan.anvilcord.core.event.FrameworkInitializationEvent;
import com.yvan.cywan.anvilcord.core.event.VirtualEventBus;
import com.yvan.cywan.anvilcord.discord.command.SlashCommand;
import com.yvan.cywan.anvilcord.discord.command.SlashCommandOrchestrator;
import com.yvan.cywan.anvilcord.discord.config.BotCoreProperties;
import com.yvan.cywan.anvilcord.discord.event.DiscordGatewayEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = {
                AnvilCordConsumerApplication.class,
                AnvilCordConsumerApplicationTests.FrameworkProbeConfiguration.class
        },
        properties = {
                "bot.core.token=",
                "bot.core.application-id="
        }
)
final class AnvilCordConsumerApplicationTests {

    private static final String EXAMPLE_PLUGIN_INITIALIZED_EVENT =
            "com.yvan.cywan.anvilcord.example.plugin.ExamplePluginInitializedEvent";
    private static final String EXAMPLE_PLUGIN_OBSERVED_BOT_READY_EVENT =
            "com.yvan.cywan.anvilcord.example.plugin.ExamplePluginObservedBotReadyEvent";
    private static final String EXAMPLE_PLUGIN_OBSERVED_DISCORD_EVENT =
            "com.yvan.cywan.anvilcord.example.plugin.ExamplePluginObservedDiscordEvent";

    @Autowired
    private VirtualEventBus eventBus;
    @Autowired
    private SlashCommandOrchestrator slashCommandOrchestrator;
    @Autowired
    private BotCoreProperties botCoreProperties;
    @Autowired
    private List<SlashCommand> slashCommands;
    @Autowired
    private FrameworkInitializationProbe frameworkInitializationProbe;

    @Test
    void starterCanBeConsumedAsAPluginHostFramework() throws InterruptedException {
        assertThat(eventBus).isNotNull();
        assertThat(botCoreProperties.hasToken()).isFalse();
        assertThat(slashCommands)
                .extracting(command -> command.commandRequest().name())
                .containsExactlyInAnyOrder("consumer-echo", "example-runtime", "ping");
        assertThat(slashCommandOrchestrator.commandNames())
                .containsExactlyInAnyOrder("consumer-echo", "example-runtime", "ping");

        FrameworkInitializationEvent initializedEvent = frameworkInitializationProbe.awaitInitialization();
        assertThat(initializedEvent.slashCommandNames())
                .containsExactlyInAnyOrder("consumer-echo", "example-runtime", "ping");
        assertThat(initializedEvent.registeredEventTypeCount()).isPositive();
        assertThat(initializedEvent.occurredAt()).isNotNull();

        assertThat(frameworkInitializationProbe.awaitPluginInitialized().getClass().getName())
                .isEqualTo(EXAMPLE_PLUGIN_INITIALIZED_EVENT);

        eventBus.publish(new BotReadyEvent(
                new BotUserProfile("42", "runtime-test-bot", "", "runtime-test-bot", "", true),
                Instant.now()
        ));
        assertThat(frameworkInitializationProbe.awaitPluginObservedBotReady().getClass().getName())
                .isEqualTo(EXAMPLE_PLUGIN_OBSERVED_BOT_READY_EVENT);

        eventBus.publish(new DiscordGatewayEvent(mock(ChatInputInteractionEvent.class), Instant.now()));
        assertThat(frameworkInitializationProbe.awaitPluginObservedDiscordEvent().getClass().getName())
                .isEqualTo(EXAMPLE_PLUGIN_OBSERVED_DISCORD_EVENT);
    }

    @TestConfiguration
    static class FrameworkProbeConfiguration {

        @Bean
        FrameworkInitializationProbe frameworkInitializationProbe(VirtualEventBus eventBus) {
            return new FrameworkInitializationProbe(eventBus);
        }
    }

    static final class FrameworkInitializationProbe {

        private final CountDownLatch initialized = new CountDownLatch(1);
        private final CountDownLatch pluginInitialized = new CountDownLatch(1);
        private final CountDownLatch pluginObservedBotReady = new CountDownLatch(1);
        private final CountDownLatch pluginObservedDiscordEvent = new CountDownLatch(1);
        private final AtomicReference<FrameworkInitializationEvent> event = new AtomicReference<>();
        private final AtomicReference<BotEvent> pluginInitializedEvent = new AtomicReference<>();
        private final AtomicReference<BotEvent> pluginObservedBotReadyEvent = new AtomicReference<>();
        private final AtomicReference<BotEvent> pluginObservedDiscordEventValue = new AtomicReference<>();

        FrameworkInitializationProbe(VirtualEventBus eventBus) {
            eventBus.registerListener(BotEvent.class, this::recordPluginEvent);
            eventBus.registerListener(FrameworkInitializationEvent.class, initializedEvent -> {
                event.set(initializedEvent);
                initialized.countDown();
            });
        }

        FrameworkInitializationEvent awaitInitialization() throws InterruptedException {
            assertThat(initialized.await(5, TimeUnit.SECONDS)).isTrue();
            return event.get();
        }

        BotEvent awaitPluginInitialized() throws InterruptedException {
            assertThat(pluginInitialized.await(5, TimeUnit.SECONDS)).isTrue();
            return pluginInitializedEvent.get();
        }

        BotEvent awaitPluginObservedBotReady() throws InterruptedException {
            assertThat(pluginObservedBotReady.await(5, TimeUnit.SECONDS)).isTrue();
            return pluginObservedBotReadyEvent.get();
        }

        BotEvent awaitPluginObservedDiscordEvent() throws InterruptedException {
            assertThat(pluginObservedDiscordEvent.await(5, TimeUnit.SECONDS)).isTrue();
            return pluginObservedDiscordEventValue.get();
        }

        private void recordPluginEvent(BotEvent event) {
            String eventTypeName = event.getClass().getName();
            if (EXAMPLE_PLUGIN_INITIALIZED_EVENT.equals(eventTypeName)) {
                pluginInitializedEvent.set(event);
                pluginInitialized.countDown();
            } else if (EXAMPLE_PLUGIN_OBSERVED_BOT_READY_EVENT.equals(eventTypeName)) {
                pluginObservedBotReadyEvent.set(event);
                pluginObservedBotReady.countDown();
            } else if (EXAMPLE_PLUGIN_OBSERVED_DISCORD_EVENT.equals(eventTypeName)) {
                pluginObservedDiscordEventValue.set(event);
                pluginObservedDiscordEvent.countDown();
            }
        }
    }
}

