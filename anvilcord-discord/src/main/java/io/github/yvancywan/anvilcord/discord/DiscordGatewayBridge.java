package io.github.yvancywan.anvilcord.discord;

import module java.base;

import io.github.yvancywan.anvilcord.core.event.BotReadyEvent;
import io.github.yvancywan.anvilcord.core.event.BotUserProfile;
import io.github.yvancywan.anvilcord.core.event.GatewayDisconnectEvent;
import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;
import io.github.yvancywan.anvilcord.discord.config.BotCoreProperties;
import io.github.yvancywan.anvilcord.discord.event.DiscordBotActions;
import io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvent;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.DisconnectEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import lombok.NonNull;
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
@SuppressWarnings("deprecation")
public final class DiscordGatewayBridge implements SmartLifecycle {

    @NonNull
    private final BotCoreProperties properties;
    @NonNull
    private final VirtualEventBus eventBus;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final List<Disposable> subscriptions = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile GatewayDiscordClient gatewayClient;
    private volatile Thread gatewayKeepAliveThread;

    public DiscordGatewayBridge(@NonNull BotCoreProperties properties, @NonNull VirtualEventBus eventBus) {
        this.properties = properties;
        this.eventBus = eventBus;
        registerBotActionListeners();
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
            startGatewayKeepAlive(gatewayClient);
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
        Thread keepAliveThread = gatewayKeepAliveThread;
        gatewayClient = null;
        gatewayKeepAliveThread = null;
        if (client != null) {
            client.logout().block(Duration.ofSeconds(10));
        }
        waitForGatewayKeepAliveThread(keepAliveThread);

        virtualThreadExecutor.shutdown();
        log.info("Discord gateway bridge stopped");
    }

    @Override
    public void stop(@NonNull Runnable callback) {
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

    private void startGatewayKeepAlive(GatewayDiscordClient client) {
        gatewayKeepAliveThread = Thread.ofPlatform()
                .daemon(false)
                .name("anvilcord-discord-gateway-keepalive")
                .start(() -> awaitGatewayDisconnect(client));
    }

    private void awaitGatewayDisconnect(GatewayDiscordClient client) {
        try {
            client.onDisconnect().block();
        } catch (RuntimeException exception) {
            if (running.get()) {
                log.warn("Discord gateway keep-alive ended with an error", exception);
            }
        } finally {
            running.set(false);
        }
    }

    private void waitForGatewayKeepAliveThread(Thread keepAliveThread) {
        if (keepAliveThread == null || keepAliveThread == Thread.currentThread()) {
            return;
        }

        try {
            keepAliveThread.join(Duration.ofSeconds(10));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for Discord gateway keep-alive thread to stop");
        }
    }

    private void publishGatewayEvent(Event event) {
        eventBus.publish(new DiscordGatewayEvent(event, Instant.now()));
        DiscordGatewayEventMapper.map(event, Instant.now()).forEach(eventBus::publish);
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
                        "",
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

    private void registerBotActionListeners() {
        eventBus.registerListener(DiscordBotActions.SendChannelMessage.class, action -> executeAction(
                "SendChannelMessage", action.correlationId(), () -> sendChannelMessage(action)
        ));
        eventBus.registerListener(DiscordBotActions.SendDirectMessage.class, action -> executeAction(
                "SendDirectMessage", action.correlationId(), () -> sendDirectMessage(action)
        ));
        eventBus.registerListener(DiscordBotActions.EditMessage.class, action -> executeAction(
                "EditMessage", action.correlationId(), () -> editMessage(action)
        ));
        eventBus.registerListener(DiscordBotActions.DeleteMessage.class, action -> executeAction(
                "DeleteMessage", action.correlationId(), () -> deleteMessage(action)
        ));
        eventBus.registerListener(DiscordBotActions.AddReaction.class, action -> executeAction(
                "AddReaction", action.correlationId(), () -> addReaction(action)
        ));
        eventBus.registerListener(DiscordBotActions.RemoveSelfReaction.class, action -> executeAction(
                "RemoveSelfReaction", action.correlationId(), () -> removeSelfReaction(action)
        ));
        eventBus.registerListener(DiscordBotActions.StartTyping.class, action -> executeAction(
                "StartTyping", action.correlationId(), () -> startTyping(action)
        ));
    }

    private void executeAction(String actionType, String correlationId, Supplier<String> action) {
        virtualThreadExecutor.submit(() -> {
            try {
                String resultId = action.get();
                eventBus.publish(new DiscordBotActions.ActionSucceeded(actionType, correlationId, resultId, Instant.now()));
            } catch (Throwable throwable) {
                log.warn("Discord bot action {} failed", actionType, throwable);
                eventBus.publish(new DiscordBotActions.ActionFailed(actionType, correlationId, throwable.getMessage(), Instant.now()));
            }
        });
    }

    private String sendChannelMessage(DiscordBotActions.SendChannelMessage action) {
        Message message = requireMessageChannel(action.channelId())
                .createMessage(MessageCreateSpec.create().withContent(action.content()))
                .block(Duration.ofSeconds(10));
        return message == null ? "" : message.getId().asString();
    }

    private String sendDirectMessage(DiscordBotActions.SendDirectMessage action) {
        Message message = requireGatewayClient()
                .getUserById(Snowflake.of(action.userId()))
                .flatMap(User::getPrivateChannel)
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.create().withContent(action.content())))
                .block(Duration.ofSeconds(10));
        return message == null ? "" : message.getId().asString();
    }

    private String editMessage(DiscordBotActions.EditMessage action) {
        Message message = requireGatewayClient()
                .getMessageById(Snowflake.of(action.channelId()), Snowflake.of(action.messageId()))
                .flatMap(existing -> existing.edit(MessageEditSpec.create().withContent(action.content())))
                .block(Duration.ofSeconds(10));
        return message == null ? action.messageId() : message.getId().asString();
    }

    private String deleteMessage(DiscordBotActions.DeleteMessage action) {
        requireGatewayClient()
                .getMessageById(Snowflake.of(action.channelId()), Snowflake.of(action.messageId()))
                .flatMap(message -> action.reason().isBlank() ? message.delete() : message.delete(action.reason()))
                .block(Duration.ofSeconds(10));
        return action.messageId();
    }

    private String addReaction(DiscordBotActions.AddReaction action) {
        requireGatewayClient()
                .getMessageById(Snowflake.of(action.channelId()), Snowflake.of(action.messageId()))
                .flatMap(message -> message.addReaction(ReactionEmoji.unicode(action.unicodeEmoji())))
                .block(Duration.ofSeconds(10));
        return action.messageId();
    }

    private String removeSelfReaction(DiscordBotActions.RemoveSelfReaction action) {
        requireGatewayClient()
                .getMessageById(Snowflake.of(action.channelId()), Snowflake.of(action.messageId()))
                .flatMap(message -> message.removeSelfReaction(ReactionEmoji.unicode(action.unicodeEmoji())))
                .block(Duration.ofSeconds(10));
        return action.messageId();
    }

    private String startTyping(DiscordBotActions.StartTyping action) {
        requireMessageChannel(action.channelId()).type().block(Duration.ofSeconds(10));
        return action.channelId();
    }

    private MessageChannel requireMessageChannel(String channelId) {
        Channel channel = requireGatewayClient()
                .getChannelById(Snowflake.of(channelId))
                .block(Duration.ofSeconds(10));
        if (channel instanceof MessageChannel messageChannel) {
            return messageChannel;
        }
        throw new IllegalArgumentException("Discord channel " + channelId + " is not a message channel");
    }

    private GatewayDiscordClient requireGatewayClient() {
        GatewayDiscordClient client = gatewayClient;
        if (client == null) {
            throw new IllegalStateException("Discord gateway is not connected; cannot execute bot action");
        }
        return client;
    }
}
