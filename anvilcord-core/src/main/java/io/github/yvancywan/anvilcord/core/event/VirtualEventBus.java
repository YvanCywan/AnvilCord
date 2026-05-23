package io.github.yvancywan.anvilcord.core.event;

import module java.base;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Thread-safe, virtual-thread-backed event bus for framework and plugin events.
 *
 * <p>Publishing is intentionally imperative: the caller hands the bus a concrete
 * event, the bus snapshots matching listeners, submits each listener to its own
 * virtual thread, and waits for all listeners to finish. This keeps ordering and
 * failure behavior easy to reason about while still allowing blocking listener
 * code to park cheaply on Project Loom virtual threads.</p>
 */
@Slf4j
@Component
public final class VirtualEventBus {

    private final ConcurrentMap<Class<? extends BotEvent>, CopyOnWriteArrayList<Consumer<? extends BotEvent>>> listeners =
            new ConcurrentHashMap<>();
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Publishes an event to every listener registered for the event's concrete
     * type or any assignable supertype.
     *
     * @param event immutable event payload to distribute
     */
    public void publish(BotEvent event) {
        Objects.requireNonNull(event, "event");

        List<Consumer<? extends BotEvent>> matchingListeners = listeners.entrySet()
                .stream()
                .filter(entry -> entry.getKey().isAssignableFrom(event.getClass()))
                .flatMap(entry -> entry.getValue().stream())
                .toList();

        if (matchingListeners.isEmpty()) {
            log.debug("Dispatching BotEvent {} occurredAt={} with no registered listeners",
                    event.getClass().getName(), event.occurredAt());
            return;
        }

        log.debug("Dispatching BotEvent {} occurredAt={} to {} listener(s)",
                event.getClass().getName(), event.occurredAt(), matchingListeners.size());
        List<Future<?>> futures = new ArrayList<>(matchingListeners.size());
        for (Consumer<? extends BotEvent> listener : matchingListeners) {
            futures.add(virtualThreadExecutor.submit(() -> {
                log.trace("Delivering BotEvent {} to listener {}",
                        event.getClass().getName(), listener.getClass().getName());
                invokeListener(listener, event);
            }));
        }

        waitForListeners(event, futures);
        log.trace("Finished dispatching BotEvent {} to {} listener(s)",
                event.getClass().getName(), matchingListeners.size());
    }

    /**
     * Registers a listener for a framework event type.
     *
     * @param eventType class token used for dispatch matching
     * @param listener  imperative listener that may block freely on virtual threads
     * @param <T>       event subtype handled by the listener
     */
    public <T extends BotEvent> void registerListener(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");

        listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("Registered BotEvent listener for {}", eventType.getName());
    }

    /**
     * @return number of event-type buckets with at least one listener.
     */
    public int registeredEventTypeCount() {
        return listeners.size();
    }

    @SuppressWarnings("unchecked")
    private static <T extends BotEvent> void invokeListener(Consumer<? extends BotEvent> listener, BotEvent event) {
        ((Consumer<T>) listener).accept((T) event);
    }

    private static void waitForListeners(BotEvent event, List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while publishing " + event.getClass().getName(), exception);
            } catch (ExecutionException exception) {
                log.error("BotEvent listener failed while handling {}", event.getClass().getName(), exception.getCause());
            }
        }
    }

    /**
     * Closes the virtual-thread executor during Spring shutdown.
     */
    @PreDestroy
    public void close() {
        virtualThreadExecutor.close();
    }
}


