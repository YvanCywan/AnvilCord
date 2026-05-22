package io.github.yvancywan.anvilcord.discord;

import module java.base;

import io.github.yvancywan.anvilcord.core.event.BotReadyEvent;
import io.github.yvancywan.anvilcord.core.event.BotUserProfile;
import io.github.yvancywan.anvilcord.core.event.GatewayDisconnectEvent;
import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;
import io.github.yvancywan.anvilcord.discord.config.BotCoreProperties;
import io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvent;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.DisconnectEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

/**
 * Owns the Discord gateway lifecycle and adapts Discord4J's native event stream
 * into this framework's virtual-threaded imperative runtime.
 *
 * <p>Discord4J is reactive internally. This class confines that model to the
 * adapter boundary: subscriptions do no business work and immediately submit raw
 * gateway events to virtual threads where the rest of the framework runs normal
 * blocking Java.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class DiscordGatewayBridge implements SmartLifecycle {

    @NonNull
    private final BotCoreProperties properties;
    @NonNull
    private final VirtualEventBus eventBus;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final List<Disposable> subscriptions = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile GatewayDiscordClient gatewayClient;

    /**
     * @return the connected gateway client when the bot is online.
     */
    public Optional<GatewayDiscordClient> gatewayClient() {
        return Optional.ofNullable(gatewayClient);
    }

    @Override
    public void start() {
        if (!properties.hasToken()) {
            log.warn("bot.core.token is blank; Discord gateway connection is disabled for this run");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            log.info("Connecting to Discord gateway using a virtual-threaded framework bridge");
            gatewayClient = DiscordClient.create(properties.requireToken()).login().block();
            if (gatewayClient == null) {
                throw new IllegalStateException("Discord4J returned no GatewayDiscordClient from login()");
            }
            subscribeToGatewayEvents(gatewayClient);
            log.info("Discord gateway bridge started");
        } catch (RuntimeException exception) {
            running.set(false);
            log.error("Discord gateway bridge failed to start", exception);
            throw exception;
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        subscriptions.forEach(Disposable::dispose);
        subscriptions.clear();

        GatewayDiscordClient client = gatewayClient;
        gatewayClient = null;
        if (client != null) {
            client.logout().block(Duration.ofSeconds(10));
        }

        virtualThreadExecutor.shutdown();
        log.info("Discord gateway bridge stopped");
    }

    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void subscribeToGatewayEvents(GatewayDiscordClient client) {
        subscriptions.add(client.on(Event.class).subscribe(
                event -> virtualThreadExecutor.submit(() -> publishGatewayEvent(event)),
                error -> log.error("Discord gateway event subscription failed", error)
        ));
    }

    private void publishGatewayEvent(Event event) {
        eventBus.publish(new DiscordGatewayEvent(event, Instant.now()));
        if (event instanceof ReadyEvent readyEvent) {
            publishReadyEvent(readyEvent);
        } else if (event instanceof DisconnectEvent disconnectEvent) {
            publishDisconnectEvent(disconnectEvent);
        }
    }

    private void publishReadyEvent(ReadyEvent event) {
        User self = event.getSelf();
        eventBus.publish(new BotReadyEvent(
                new BotUserProfile(
                        self.getId().asString(),
                        self.getUsername(),
                        self.getDiscriminator(),
                        self.getTag(),
                        self.getAvatarUrl(),
                        self.isBot()
                ),
                Instant.now()
        ));
    }

    private void publishDisconnectEvent(DisconnectEvent event) {
        eventBus.publish(new GatewayDisconnectEvent(
                event.getStatus().getCode(),
                event.getStatus().getReason().orElse(""),
                event.getCause().map(Throwable::getMessage).orElse(""),
                event.getShardInfo().format(),
                Instant.now()
        ));
    }
}
