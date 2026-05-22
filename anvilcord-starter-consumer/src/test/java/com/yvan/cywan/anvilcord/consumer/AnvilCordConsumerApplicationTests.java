package com.yvan.cywan.anvilcord.consumer;

import module java.base;

import com.yvan.cywan.anvilcord.core.event.FrameworkInitializationEvent;
import com.yvan.cywan.anvilcord.core.event.VirtualEventBus;
import com.yvan.cywan.anvilcord.discord.command.SlashCommand;
import com.yvan.cywan.anvilcord.discord.command.SlashCommandOrchestrator;
import com.yvan.cywan.anvilcord.discord.config.BotCoreProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

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
                .containsExactly("ping");
        assertThat(slashCommandOrchestrator.commandNames()).containsExactly("ping");

        FrameworkInitializationEvent initializedEvent = frameworkInitializationProbe.awaitInitialization();
        assertThat(initializedEvent.slashCommandNames()).containsExactly("ping");
        assertThat(initializedEvent.registeredEventTypeCount()).isPositive();
        assertThat(initializedEvent.occurredAt()).isNotNull();
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
        private final AtomicReference<FrameworkInitializationEvent> event = new AtomicReference<>();

        FrameworkInitializationProbe(VirtualEventBus eventBus) {
            eventBus.registerListener(FrameworkInitializationEvent.class, initializedEvent -> {
                event.set(initializedEvent);
                initialized.countDown();
            });
        }

        FrameworkInitializationEvent awaitInitialization() throws InterruptedException {
            assertThat(initialized.await(5, TimeUnit.SECONDS)).isTrue();
            return event.get();
        }
    }
}

