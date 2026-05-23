# AnvilCord Discord events

`anvilcord-discord` bridges Discord4J gateway traffic into AnvilCord's shared `VirtualEventBus` and consumes bot action events published by plugins.

Plugins can still listen to `DiscordGatewayEvent` for the raw Discord4J event object, but the preferred API is the stable payload catalog in `io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvents`.

## Consuming gateway events

Register listeners from a plugin initializer:

```java
@Override
public void initialize(AnvilCordPluginContext context) {
    context.registerListener(DiscordGatewayEvents.MessageCreated.class, event -> {
        String channelId = event.message().channelId();
        String content = event.message().content();
        // react to the message
    });
}
```

Every gateway event record implements `BotEvent` and includes `Instant occurredAt`.

## Seeing event dispatch logs

Event-bus dispatch summaries are emitted at `INFO` from `VirtualEventBus`, so they are visible with Spring Boot's default root logging level. Discord gateway adapter summaries are emitted at `DEBUG`; enable that category in a host application's Spring configuration when developing plugins:

```yaml
logging:
  level:
    io.github.yvancywan.anvilcord.discord.DiscordGatewayBridge: DEBUG
```

Use `TRACE` for `io.github.yvancywan.anvilcord.core.event.VirtualEventBus` when you also want individual listener-delivery logs. Use `TRACE` for `io.github.yvancywan.anvilcord.discord.DiscordGatewayBridge` when you want mapped Discord-event details. Discord gateway traffic can be high volume, so `TRACE` is best used temporarily during local debugging.

## Common snapshots

These immutable snapshot records are reused by gateway events.

### `UserSnapshot`

| Field | Meaning |
| --- | --- |
| `id` | Discord user snowflake as a string. |
| `username` | Current username. |
| `discriminator` | User discriminator returned by Discord4J. |
| `tag` | Discord4J tag representation. |
| `globalName` | Display/global name when provided by Discord. |
| `avatarUrl` | Current avatar URL. |
| `bot` | Whether the user is a bot account. |

### `MemberSnapshot`

| Field | Meaning |
| --- | --- |
| `user` | `UserSnapshot` for the guild member. |
| `guildId` | Guild snowflake as a string. |
| `displayName` | Effective guild display name. |
| `nickname` | Guild nickname, or blank when absent. |
| `roleIds` | Immutable set of role snowflakes. |
| `joinedAt` | Guild join timestamp when cached/provided. |

### `GuildSnapshot`

| Field | Meaning |
| --- | --- |
| `id` | Guild snowflake. |
| `name` | Guild name. |
| `ownerId` | Owner user snowflake. |
| `memberCount` | Member count reported by Discord4J. |
| `unavailable` | Whether Discord marks the guild unavailable. |

### `ChannelSnapshot`

| Field | Meaning |
| --- | --- |
| `id` | Channel snowflake. |
| `guildId` | Guild snowflake, blank for non-guild channels. |
| `name` | Channel name when available. |
| `type` | Discord4J channel type string. |

### `RoleSnapshot`

| Field | Meaning |
| --- | --- |
| `id` | Role snowflake. |
| `guildId` | Guild snowflake. |
| `name` | Role name. |
| `rawPosition` | Raw Discord role position. |
| `permissions` | Discord4J permission set string. |
| `managed` | Whether Discord manages the role. |
| `mentionable` | Whether the role is mentionable. |

### `MessageSnapshot`

| Field | Meaning |
| --- | --- |
| `id` | Message snowflake. |
| `channelId` | Channel snowflake. |
| `guildId` | Guild snowflake, blank for DM messages. |
| `author` | `UserSnapshot` when Discord4J provides one. |
| `content` | Message content visible to the bot. Requires the configured Discord intent. |
| `timestamp` | Message creation timestamp. |
| `editedTimestamp` | Edit timestamp when present. |
| `pinned` | Whether the message is pinned. |
| `tts` | Whether the message is text-to-speech. |
| `userMentionIds` | User mention snowflakes. |
| `roleMentionIds` | Role mention snowflakes. |

### `EmojiSnapshot`

| Field | Meaning |
| --- | --- |
| `id` | Custom emoji snowflake, or blank for Unicode emoji. |
| `name` | Custom emoji name or Unicode glyph. |
| `animated` | Whether a custom emoji is animated. |
| `raw` | Discord4J raw/format string for the emoji. |

## Published gateway events

### Message events

| Event record | Payload |
| --- | --- |
| `MessageCreated` | `MessageSnapshot message`, nullable `MemberSnapshot member`, `occurredAt`. |
| `MessageUpdated` | `messageId`, `channelId`, `guildId`, `currentContent`, `contentChanged`, `embedsChanged`, nullable cached previous `MessageSnapshot`, `occurredAt`. |
| `MessageDeleted` | `messageId`, `channelId`, `guildId`, nullable cached `MessageSnapshot`, `occurredAt`. |
| `MessageBulkDeleted` | `Set<String> messageIds`, `channelId`, `guildId`, `occurredAt`. |

### Reaction events

| Event record | Payload |
| --- | --- |
| `ReactionAdded` | `userId`, `channelId`, `messageId`, `guildId`, `EmojiSnapshot emoji`, nullable `MemberSnapshot member`, `messageAuthorId`, `occurredAt`. |
| `ReactionRemoved` | `userId`, `channelId`, `messageId`, `guildId`, `EmojiSnapshot emoji`, `occurredAt`. |
| `ReactionsCleared` | `channelId`, `messageId`, `guildId`, `occurredAt`. |
| `ReactionEmojiCleared` | `channelId`, `messageId`, `guildId`, `EmojiSnapshot emoji`, `occurredAt`. |

### Guild and member events

| Event record | Payload |
| --- | --- |
| `GuildCreated` | `GuildSnapshot guild`, `occurredAt`. |
| `GuildUpdated` | Current `GuildSnapshot`, nullable previous `GuildSnapshot`, `occurredAt`. |
| `GuildDeleted` | `guildId`, nullable cached `GuildSnapshot`, `unavailable`, `occurredAt`. |
| `MemberJoined` | `MemberSnapshot member`, `occurredAt`. |
| `MemberLeft` | `guildId`, `UserSnapshot user`, nullable cached `MemberSnapshot`, `occurredAt`. |
| `MemberUpdated` | `guildId`, `memberId`, `currentRoleIds`, `currentNickname`, `joinedAt`, `pending`, nullable cached previous `MemberSnapshot`, `occurredAt`. |

### Channel events

| Event record | Payload |
| --- | --- |
| `ChannelCreated` | `ChannelSnapshot channel`, `occurredAt`. Published for text channels directly and for other Discord4J channel create events with a `getChannel()` accessor. |
| `ChannelUpdated` | Current `ChannelSnapshot`, nullable previous `ChannelSnapshot`, `occurredAt`. Published for text channels directly and for other Discord4J channel update events with `getCurrent()`/`getOld()` accessors. |
| `ChannelDeleted` | `ChannelSnapshot channel`, `occurredAt`. Published for text channels directly and for other Discord4J channel delete events with a `getChannel()` accessor. |
| `PinsUpdated` | `channelId`, `guildId`, nullable `lastPinTimestamp`, `occurredAt`. |
| `TypingStarted` | `channelId`, `guildId`, `userId`, nullable `MemberSnapshot member`, `startedAt`, `occurredAt`. |

### Role and user events

| Event record | Payload |
| --- | --- |
| `RoleCreated` | `RoleSnapshot role`, `occurredAt`. |
| `RoleUpdated` | Current `RoleSnapshot`, nullable previous `RoleSnapshot`, `occurredAt`. |
| `RoleDeleted` | `guildId`, `roleId`, nullable cached `RoleSnapshot`, `occurredAt`. |
| `UserUpdated` | Current `UserSnapshot`, nullable previous `UserSnapshot`, `occurredAt`. |
| `PresenceUpdated` | `guildId`, `userId`, `status`, nullable old `UserSnapshot`, `occurredAt`. |

### Voice, invite, webhook, and interaction events

| Event record | Payload |
| --- | --- |
| `VoiceStateUpdated` | `guildId`, `userId`, `channelId`, `join`, `leave`, `move`, `occurredAt`. |
| `InviteCreated` | `code`, `channelId`, `guildId`, nullable `UserSnapshot inviter`, `uses`, `maxUses`, `maxAgeSeconds`, `temporary`, nullable `expiresAt`, `occurredAt`. |
| `InviteDeleted` | `code`, `channelId`, `guildId`, `occurredAt`. |
| `WebhooksUpdated` | `guildId`, `channelId`, `occurredAt`. |
| `InteractionReceived` | `interactionId`, `applicationId`, `channelId`, `guildId`, `userId`, `interactionType`, `occurredAt`. This event is for generic interaction observation. Slash commands also publish `SlashCommandInvocationEvent`. |

## Slash-command model and invocation events

`SlashCommand` is a model record made from standard Java values: `name`, `description`, and optional `List<SlashCommand.Option>`. The Discord module adapts this model into Discord4J `ApplicationCommandRequest` objects internally.

Plugins can contribute commands by publishing `SlashCommandRegistrationEvent` during plugin initialization:

```java
SlashCommand command = new SlashCommand("announce", "Publishes an announcement.");
context.publish(new SlashCommandRegistrationEvent(command, Instant.now()));
```

When Discord invokes a registered chat-input command, `SlashCommandOrchestrator` publishes `SlashCommandInvocationEvent` with:

| Field | Meaning |
| --- | --- |
| `commandName` | Registered command name. |
| `interactionId` | Discord interaction snowflake used for `RespondToInteraction`. |
| `channelId` | Channel where the command was invoked. |
| `guildId` | Guild snowflake, blank for non-guild contexts. |
| `userId` | Invoking user snowflake. |
| `options` | Immutable `Map<String, String>` of option names to raw Discord values. |
| `occurredAt` | Time AnvilCord published the invocation. |

## Raw gateway fallback

`DiscordGatewayEvent` is still published for every Discord4J `Event` before mapped events are published:

| Field | Meaning |
| --- | --- |
| `discordEvent` | Raw Discord4J gateway event object. |
| `occurredAt` | Time AnvilCord received and published it. |

Use this only when Discord4J exposes an event that does not yet have a stable AnvilCord payload.

## Publishing bot actions

Plugins request bot side effects by publishing records from `io.github.yvancywan.anvilcord.discord.event.DiscordBotActions`:

```java
context.publish(new DiscordBotActions.SendChannelMessage(
        "123456789012345678",
        "Announcement deployed!",
        "announcement-42",
        Instant.now()
));
```

The bridge executes actions only when the Discord gateway is connected. Each action has a `correlationId`; if blank, AnvilCord generates one.

### Action events consumed by the bridge

| Action record | Required payload | Effect |
| --- | --- | --- |
| `SendChannelMessage` | `channelId`, `content`, `correlationId`, `occurredAt` | Sends a text message to a Discord message channel. |
| `SendDirectMessage` | `userId`, `content`, `correlationId`, `occurredAt` | Opens/uses a DM channel and sends a text message. |
| `EditMessage` | `channelId`, `messageId`, `content`, `correlationId`, `occurredAt` | Edits a message addressable by channel/message id. |
| `DeleteMessage` | `channelId`, `messageId`, optional `reason`, `correlationId`, `occurredAt` | Deletes a message, using the reason when supplied. |
| `AddReaction` | `channelId`, `messageId`, `unicodeEmoji`, `correlationId`, `occurredAt` | Adds a Unicode emoji reaction. |
| `RemoveSelfReaction` | `channelId`, `messageId`, `unicodeEmoji`, `correlationId`, `occurredAt` | Removes the bot's own Unicode emoji reaction. |
| `StartTyping` | `channelId`, `correlationId`, `occurredAt` | Emits a typing indicator in a message channel. |
| `RespondToInteraction` | `interactionId`, `content`, `correlationId`, `occurredAt` | Replies to a pending slash-command interaction published as `SlashCommandInvocationEvent`. |

### Action result events

| Result event | Payload | Meaning |
| --- | --- | --- |
| `ActionSucceeded` | `actionType`, `correlationId`, `resultId`, `occurredAt` | The requested Discord side effect completed. `resultId` is the created/affected message id when applicable, otherwise the affected channel/message id. |
| `ActionFailed` | `actionType`, `correlationId`, `errorMessage`, `occurredAt` | The requested action could not be completed. Common causes are a disconnected gateway, missing permissions, invalid snowflakes, or a non-message channel. |

## Discord intents and permissions

Discord only sends events and fields the bot is allowed to receive. For announcement-style plugins, check these common requirements:

- Message content requires the Message Content privileged intent when reading message text outside interactions.
- Member and presence events require the corresponding gateway intents in the Discord developer portal and bot login configuration.
- Send/edit/delete/reaction actions require normal Discord channel permissions for the bot account.

