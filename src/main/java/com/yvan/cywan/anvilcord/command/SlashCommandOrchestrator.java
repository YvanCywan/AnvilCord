package com.yvan.cywan.anvilcord.command;

import module java.base;

import com.yvan.cywan.anvilcord.core.config.BotCoreProperties;
import com.yvan.cywan.anvilcord.discord.DiscordGatewayBridge;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * Discovers every Spring-managed {@link SlashCommand}, synchronizes their
 * metadata with Discord, and routes live chat-input interactions to the matching
 * command implementation.
 */
@Service
public final class SlashCommandOrchestrator implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlashCommandOrchestrator.class);

    private final BotCoreProperties properties;
    private final Map<String, SlashCommand> commandsByName;
    private final List<ApplicationCommandRequest> commandRequests;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public SlashCommandOrchestrator(
            List<SlashCommand> slashCommands,
            BotCoreProperties properties,
            DiscordGatewayBridge gatewayBridge
    ) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.commandsByName = indexCommands(slashCommands == null ? List.of() : slashCommands);
        this.commandRequests = commandsByName.values().stream()
                .map(SlashCommand::commandRequest)
                .toList();

        gatewayBridge.registerChatInputInteractionListener(this::dispatchInteraction);
        LOGGER.info("Discovered {} slash command(s): {}", commandsByName.size(), commandsByName.keySet());
    }

    /**
     * Blocking command synchronization executed once during Spring startup.
     */
    @Override
    public void run(String... args) {
        if (commandRequests.isEmpty()) {
            LOGGER.info("No slash commands discovered; skipping Discord command synchronization");
            return;
        }
        if (!properties.hasToken()) {
            LOGGER.warn("bot.core.token is blank; skipping Discord command synchronization");
            return;
        }

        RestClient restClient = RestClient.create(properties.requireToken());
        long applicationId = properties.applicationIdAsLong()
                .orElseGet(() -> Objects.requireNonNull(
                        restClient.getApplicationId().block(),
                        "Discord REST API returned no application id"
                ));

        LOGGER.info("Synchronizing {} global slash command(s) for application {}", commandRequests.size(), applicationId);
        restClient.getApplicationService()
                .bulkOverwriteGlobalApplicationCommand(applicationId, commandRequests)
                .collectList()
                .block();
        LOGGER.info("Slash command synchronization complete");
    }

    /**
     * @return immutable command names for lifecycle diagnostics.
     */
    public List<String> commandNames() {
        return List.copyOf(commandsByName.keySet());
    }

    private void dispatchInteraction(ChatInputInteractionEvent event) {
        virtualThreadExecutor.submit(() -> executeSafely(event));
    }

    private void executeSafely(ChatInputInteractionEvent event) {
        String commandName = event.getCommandName();
        SlashCommand command = commandsByName.get(commandName);
        if (command == null) {
            LOGGER.warn("Received unknown slash command interaction: {}", commandName);
            replyBestEffort(event, "Unknown command: " + commandName);
            return;
        }

        try {
            command.execute(event);
        } catch (Throwable throwable) {
            LOGGER.error("Slash command '{}' failed", commandName, throwable);
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
            LOGGER.warn("Unable to send slash-command error response", replyFailure);
        }
    }

    @PreDestroy
    public void close() {
        virtualThreadExecutor.close();
    }
}


