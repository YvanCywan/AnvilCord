package io.github.yvancywan.anvilcord.discord.command;

import module java.base;

import io.github.yvancywan.anvilcord.core.event.VirtualEventBus;
import io.github.yvancywan.anvilcord.discord.config.BotCoreProperties;
import io.github.yvancywan.anvilcord.discord.event.DiscordBotActions;
import io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import jakarta.annotation.PreDestroy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * Discovers framework {@link SlashCommandDefinition} models, synchronizes their metadata
 * with Discord, and publishes live chat-input interactions to the event bus.
 */
@Slf4j
@Service
public final class SlashCommandOrchestrator implements CommandLineRunner {

    private final BotCoreProperties properties;
    private final VirtualEventBus eventBus;
    private final ConcurrentMap<String, SlashCommandDefinition> commandsByName = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ChatInputInteractionEvent> pendingInteractions = new ConcurrentHashMap<>();
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public SlashCommandOrchestrator(
            List<SlashCommandDefinition> slashCommands,
            BotCoreProperties properties,
            VirtualEventBus eventBus
    ) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        registerCommands(slashCommands == null ? List.of() : slashCommands);

        eventBus.registerListener(DiscordGatewayEvent.class, this::dispatchGatewayEvent);
        eventBus.registerListener(SlashCommandRegistrationEvent.class, this::registerCommand);
        eventBus.registerListener(DiscordBotActions.RespondToInteraction.class, this::respondToInteraction);
        log.info("Discovered {} slash command(s): {}", commandsByName.size(), commandsByName.keySet());
    }

    /**
     * Blocking command synchronization executed once during Spring startup.
     */
    @Override
    public void run(@NonNull String... args) {
        List<ApplicationCommandRequest> commandRequests = commandRequests();
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
        return commandsByName.keySet().stream().sorted().toList();
    }

    private void dispatchGatewayEvent(DiscordGatewayEvent event) {
        if (event.discordEvent() instanceof ChatInputInteractionEvent chatInputInteractionEvent) {
            dispatchInteraction(chatInputInteractionEvent);
        }
    }

    private void dispatchInteraction(ChatInputInteractionEvent event) {
        virtualThreadExecutor.submit(() -> publishInvocation(event));
    }

    private void publishInvocation(ChatInputInteractionEvent event) {
        String commandName = event.getCommandName();
        SlashCommandDefinition command = commandsByName.get(commandName);
        if (command == null) {
            log.warn("Received unknown slash command interaction: {}", commandName);
            replyBestEffort(event, "Unknown command: " + commandName);
            return;
        }

        String interactionId = event.getInteraction().getId().asString();
        pendingInteractions.put(interactionId, event);
        log.debug("Publishing slash command invocation commandName={} interactionId={}", commandName, interactionId);
        eventBus.publish(new SlashCommandInvocationEvent(
                command.name(),
                interactionId,
                event.getInteraction().getChannelId().asString(),
                event.getInteraction().getGuildId().map(snowflake -> snowflake.asString()).orElse(""),
                event.getInteraction().getUser().getId().asString(),
                optionValues(event),
                Instant.now()
        ));
    }

    private void registerCommands(List<SlashCommandDefinition> slashCommands) {
        for (SlashCommandDefinition command : slashCommands) {
            registerCommand(command);
        }
    }

    private void registerCommand(SlashCommandRegistrationEvent event) {
        registerCommand(event.command());
    }

    private void registerCommand(SlashCommandDefinition command) {
        Objects.requireNonNull(command, "command");
        SlashCommandDefinition previous = commandsByName.putIfAbsent(command.name(), command);
        if (previous != null && !previous.equals(command)) {
            throw new IllegalStateException("Duplicate slash-command name '" + command.name() + "'");
        }
        log.debug("Registered slash command model '{}'", command.name());
    }

    private void respondToInteraction(DiscordBotActions.RespondToInteraction action) {
        virtualThreadExecutor.submit(() -> {
            ChatInputInteractionEvent event = pendingInteractions.remove(action.interactionId());
            if (event == null) {
                eventBus.publish(new DiscordBotActions.ActionFailed(
                        "RespondToInteraction",
                        action.correlationId(),
                        "No pending slash-command interaction for id " + action.interactionId(),
                        Instant.now()
                ));
                return;
            }

            try {
                event.reply(action.content()).block();
                eventBus.publish(new DiscordBotActions.ActionSucceeded(
                        "RespondToInteraction",
                        action.correlationId(),
                        action.interactionId(),
                        Instant.now()
                ));
            } catch (RuntimeException exception) {
                eventBus.publish(new DiscordBotActions.ActionFailed(
                        "RespondToInteraction",
                        action.correlationId(),
                        exception.getMessage(),
                        Instant.now()
                ));
            }
        });
    }

    private List<ApplicationCommandRequest> commandRequests() {
        return commandsByName.values().stream()
                .sorted(Comparator.comparing(SlashCommandDefinition::name))
                .map(SlashCommandOrchestrator::toDiscordRequest)
                .toList();
    }

    private static ApplicationCommandRequest toDiscordRequest(SlashCommandDefinition command) {
        var builder = ApplicationCommandRequest.builder()
                .name(command.name())
                .description(command.description());
        if (!command.options().isEmpty()) {
            builder.options(command.options().stream().map(SlashCommandOrchestrator::toDiscordOption).toList());
        }
        return builder.build();
    }

    private static ApplicationCommandOptionData toDiscordOption(SlashCommandDefinition.Option option) {
        return ApplicationCommandOptionData.builder()
                .name(option.name())
                .description(option.description())
                .type(option.type().discordType())
                .required(option.required())
                .build();
    }

    private static Map<String, String> optionValues(ChatInputInteractionEvent event) {
        Map<String, String> values = new LinkedHashMap<>();
        for (ApplicationCommandInteractionOption option : event.getOptions()) {
            option.getValue().ifPresent(value -> values.put(option.getName(), value.getRaw()));
        }
        return Map.copyOf(values);
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


