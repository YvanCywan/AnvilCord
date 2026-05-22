package io.github.yvancywan.anvilcord.core.event;

import module java.base;

/**
 * Marker contract for framework-level events distributed by {@link VirtualEventBus}.
 *
 * <p>The interface is intentionally open rather than sealed so third-party
 * modules can publish their own strongly typed events while still depending
 * only on the public core package.</p>
 */
public interface BotEvent {

    /**
     * @return the framework timestamp at which the event object was created.
     */
    Instant occurredAt();
}

