package io.github.yvancywan.anvilcord.starter;

import module java.base;

import io.github.yvancywan.anvilcord.core.event.FrameworkInitializationEvent;
import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;
import io.github.yvancywan.anvilcord.discord.command.SlashCommandOrchestrator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Emits the framework initialization event after Spring has completed component
 * scanning, dependency injection, lifecycle startup, and command-line runners.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class FrameworkInitializationPublisher {

    @NonNull
    private final VirtualEventBus eventBus;
    @NonNull
    private final SlashCommandOrchestrator slashCommandOrchestrator;

    @EventListener(ApplicationReadyEvent.class)
    public void publishFrameworkInitialized() {
        FrameworkInitializationEvent event = new FrameworkInitializationEvent(
                slashCommandOrchestrator.commandNames(),
                eventBus.registeredEventTypeCount(),
                Instant.now()
        );
        eventBus.publish(event);
        log.info("Framework initialization complete with {} slash command(s)", event.slashCommandNames().size());
    }
}

