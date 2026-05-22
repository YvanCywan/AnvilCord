package com.yvan.cywan.anvilcord.core.event;

import module java.base;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class VirtualEventBusTests {

    @Test
    void publishDispatchesMatchingListenersOnVirtualThreadsAndContinuesAfterFailure() {
        VirtualEventBus eventBus = new VirtualEventBus();
        try {
            AtomicInteger deliveries = new AtomicInteger();
            AtomicBoolean listenerRanOnVirtualThread = new AtomicBoolean(false);

            eventBus.registerListener(BotEvent.class, event -> {
                listenerRanOnVirtualThread.set(Thread.currentThread().isVirtual());
                deliveries.incrementAndGet();
            });
            eventBus.registerListener(TestEvent.class, event -> {
                throw new IllegalStateException("simulated listener failure");
            });
            eventBus.registerListener(TestEvent.class, event -> deliveries.incrementAndGet());

            eventBus.publish(new TestEvent(Instant.now()));

            assertThat(deliveries).hasValue(2);
            assertThat(listenerRanOnVirtualThread).isTrue();
            assertThat(eventBus.registeredEventTypeCount()).isEqualTo(2);
        } finally {
            eventBus.close();
        }
    }

    private record TestEvent(Instant occurredAt) implements BotEvent {
    }
}
