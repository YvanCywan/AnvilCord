package io.github.yvancywan.anvilcord.core.plugin;

import io.github.yvancywan.anvilcord.core.event.BotEvent;
import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Runtime services exposed to AnvilCord plugin entrypoints.
 */
public final class AnvilCordPluginContext {

    private final VirtualEventBus eventBus;

    public AnvilCordPluginContext(VirtualEventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    /**
     * @return the shared framework event bus for core, Discord, and plugin events
     */
    public VirtualEventBus eventBus() {
        return eventBus;
    }

    /**
     * Registers a listener for framework or plugin-defined events.
     *
     * @param eventType event class token used for dispatch matching
     * @param listener  imperative listener that may block freely on virtual threads
     * @param <T>       event subtype handled by the listener
     */
    public <T extends BotEvent> void registerListener(Class<T> eventType, Consumer<T> listener) {
        eventBus.registerListener(eventType, listener);
    }

    /**
     * Publishes a framework or plugin-defined event.
     *
     * @param event immutable event payload to distribute
     */
    public void publish(BotEvent event) {
        eventBus.publish(event);
    }
}

