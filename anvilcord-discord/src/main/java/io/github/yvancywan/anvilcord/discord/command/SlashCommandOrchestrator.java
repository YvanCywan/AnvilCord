package io.github.yvancywan.anvilcord.discord.command;

import module java.base;

import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;
import io.github.yvancywan.anvilcord.discord.config.BotCoreProperties;
import io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * Discovers every Spring-managed {@link SlashCommand}, synchronizes their
 * metadata with Discord, and routes live chat-input interactions to the matching
 * command implementation.
 */
@Slf4j
@Service
public final class SlashCommandOrchestrator implements CommandLineRunner {

    private final BotCoreProperties properties;
    private final Map<String, SlashCommand> commandsByName;
    private final List<ApplicationCommandRequest> commandRequests;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public SlashCommandOrchestrator(
            List<SlashCommand> slashCommands,
            BotCoreProperties properties,
            VirtualEventBus eventBus
    ) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.commandsByName = indexCommands(slashCommands == null ? List.of() : slashCommands);
        this.commandRequests = commandsByName.values().stream()
                .map(SlashCommand::commandRequest)
                .toList();

        eventBus.registerListener(DiscordGatewayEvent.class, this::dispatchGatewayEvent);
        log.info("Discovered {} slash command(s): {}", commandsByName.size(), commandsByName.keySet());
    }

    /**
     * Blocking command synchronization executed once during Spring startup.
     */
    @Override
    public void run(String... args) {
        if (commandRequests.isEmpty()) {
            log.info("No slash commands discovered; skipping Discord command synchronization");
            return;
        }
        if (!properties.hasToken()) {
            log.warn("bot.core.token is blank; skipping Discord command synchronization");
            return;
        }

        RestClient restClient = RestClient.create(properties.requireToken());
        long applicationId = properties.applicationIdAsLong()
                .orElseGet(() -> Objects.requireNonNull(
                        restClient.getApplicationId().block(),
                        "Discord REST API returned no application id"
                ));

        log.info("Synchronizing {} global slash command(s) for application {}", commandRequests.size(), applicationId);
        restClient.getApplicationService()
                .bulkOverwriteGlobalApplicationCommand(applicationId, commandRequests)
                .collectList()
                .block();
        log.info("Slash command synchronization complete");
    }

    /**
     * @return immutable command names for lifecycle diagnostics.
     */
    public List<String> commandNames() {
        return List.copyOf(commandsByName.keySet());
    }

    private void dispatchGatewayEvent(DiscordGatewayEvent event) {
        if (event.discordEvent() instanceof ChatInputInteractionEvent chatInputInteractionEvent) {
            dispatchInteraction(chatInputInteractionEvent);
        }
    }

    private void dispatchInteraction(ChatInputInteractionEvent event) {
        virtualThreadExecutor.submit(() -> executeSafely(event));
    }

    private void executeSafely(ChatInputInteractionEvent event) {
        String commandName = event.getCommandName();
        SlashCommand command = commandsByName.get(commandName);
        if (command == null) {
            log.warn("Received unknown slash command interaction: {}", commandName);
            replyBestEffort(event, "Unknown command: " + commandName);
            return;
        }

        try {
            command.execute(event);
        } catch (Throwable throwable) {
            log.error("Slash command '{}' failed", commandName, throwable);
            replyBestEffort(event, "Command failed. Please try again later.");
        }
    }

    private static Map<String, SlashCommand> indexCommands(List<SlashCommand> slashCommands) {
        Map<String, SlashCommand> indexed = new LinkedHashMap<>();
        for (SlashCommand command : slashCommands) {
            ApplicationCommandRequest request = Objects.requireNonNull(command.commandRequest(), "commandRequest");
            String name = request.name();
            if (name.isBlank()) {
                throw new IllegalArgumentException(command.getClass().getName() + " returned a blank slash-command name");
            }
            SlashCommand previous = indexed.putIfAbsent(name, command);
            if (previous != null) {
                throw new IllegalStateException("Duplicate slash-command name '" + name + "' from "
                        + previous.getClass().getName() + " and " + command.getClass().getName());
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
    }

    private static void replyBestEffort(ChatInputInteractionEvent event, String message) {
        try {
            event.reply(message).block();
        } catch (RuntimeException replyFailure) {
            log.warn("Unable to send slash-command error response", replyFailure);
        }
    }

    @PreDestroy
    public void close() {
        virtualThreadExecutor.close();
    }
}


