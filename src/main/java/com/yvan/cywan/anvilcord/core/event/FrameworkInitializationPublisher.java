package com.yvan.cywan.anvilcord.core.event;

import module java.base;

import com.yvan.cywan.anvilcord.command.SlashCommandOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Emits the framework initialization event after Spring has completed component
 * scanning, dependency injection, lifecycle startup, and command-line runners.
 */
@Component
public final class FrameworkInitializationPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkInitializationPublisher.class);

    private final VirtualEventBus eventBus;
    private final SlashCommandOrchestrator slashCommandOrchestrator;

    public FrameworkInitializationPublisher(VirtualEventBus eventBus, SlashCommandOrchestrator slashCommandOrchestrator) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.slashCommandOrchestrator = Objects.requireNonNull(slashCommandOrchestrator, "slashCommandOrchestrator");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishFrameworkInitialized() {
        FrameworkInitializationEvent event = new FrameworkInitializationEvent(
                slashCommandOrchestrator.commandNames(),
                eventBus.registeredEventTypeCount(),
                Instant.now()
        );
        eventBus.publish(event);
        LOGGER.info("Framework initialization complete with {} slash command(s)", event.slashCommandNames().size());
    }
}

