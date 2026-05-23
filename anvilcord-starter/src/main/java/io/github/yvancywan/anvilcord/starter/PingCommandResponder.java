package io.github.yvancywan.anvilcord.starter;

import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;
import io.github.yvancywan.anvilcord.discord.command.SlashCommandInvocationEvent;
import io.github.yvancywan.anvilcord.discord.event.DiscordBotActions;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

/** Replies to the built-in ping command through the shared event bus. */
@Component
public final class PingCommandResponder {

    private final VirtualEventBus eventBus;

    public PingCommandResponder(VirtualEventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.eventBus.registerListener(SlashCommandInvocationEvent.class, this::onSlashCommandInvoked);
    }

    private void onSlashCommandInvoked(SlashCommandInvocationEvent event) {
        if (!PingCommand.NAME.equals(event.commandName())) {
            return;
        }
        eventBus.publish(new DiscordBotActions.RespondToInteraction(
                event.interactionId(),
                "Pong!",
                "ping-" + event.interactionId(),
                Instant.now()
        ));
    }
}

