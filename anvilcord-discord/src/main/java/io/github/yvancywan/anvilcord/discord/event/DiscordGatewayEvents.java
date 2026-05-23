package io.github.yvancywan.anvilcord.discord.event;

import io.github.yvancywan.anvilcord.core.event.BotEvent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Stable AnvilCord payloads produced from Discord gateway traffic.
 *
 * <p>These records intentionally contain primitive values and immutable snapshot
 * records so plugins can react to Discord activity without depending on the
 * lifecycle or cache semantics of Discord4J entity objects.</p>
 */
@SuppressWarnings("unused")
public final class DiscordGatewayEvents {

    private DiscordGatewayEvents() {
    }

    /** Lightweight user snapshot. */
    public record UserSnapshot(
            String id,
            String username,
            String discriminator,
            String tag,
            String globalName,
            String avatarUrl,
            boolean bot
    ) {
        public UserSnapshot {
            id = requireText(id, "id");
            username = nullToEmpty(username);
            discriminator = nullToEmpty(discriminator);
            tag = nullToEmpty(tag);
            globalName = nullToEmpty(globalName);
            avatarUrl = nullToEmpty(avatarUrl);
        }
    }

    /** Lightweight member snapshot within a guild. */
    public record MemberSnapshot(
            UserSnapshot user,
            String guildId,
            String displayName,
            String nickname,
            Set<String> roleIds,
            Instant joinedAt
    ) {
        public MemberSnapshot {
            Objects.requireNonNull(user, "user");
            guildId = requireText(guildId, "guildId");
            displayName = nullToEmpty(displayName);
            nickname = nullToEmpty(nickname);
            roleIds = Set.copyOf(roleIds == null ? Set.of() : roleIds);
        }
    }

    /** Lightweight guild snapshot. */
    public record GuildSnapshot(String id, String name, String ownerId, int memberCount, boolean unavailable) {
        public GuildSnapshot {
            id = requireText(id, "id");
            name = nullToEmpty(name);
            ownerId = nullToEmpty(ownerId);
        }
    }

    /** Lightweight channel snapshot. */
    public record ChannelSnapshot(String id, String guildId, String name, String type) {
        public ChannelSnapshot {
            id = requireText(id, "id");
            guildId = nullToEmpty(guildId);
            name = nullToEmpty(name);
            type = nullToEmpty(type);
        }
    }

    /** Lightweight role snapshot. */
    public record RoleSnapshot(
            String id,
            String guildId,
            String name,
            int rawPosition,
            String permissions,
            boolean managed,
            boolean mentionable
    ) {
        public RoleSnapshot {
            id = requireText(id, "id");
            guildId = requireText(guildId, "guildId");
            name = nullToEmpty(name);
            permissions = nullToEmpty(permissions);
        }
    }

    /** Lightweight message snapshot. */
    public record MessageSnapshot(
            String id,
            String channelId,
            String guildId,
            UserSnapshot author,
            String content,
            Instant timestamp,
            Instant editedTimestamp,
            boolean pinned,
            boolean tts,
            List<String> userMentionIds,
            List<String> roleMentionIds
    ) {
        public MessageSnapshot {
            id = requireText(id, "id");
            channelId = requireText(channelId, "channelId");
            guildId = nullToEmpty(guildId);
            content = nullToEmpty(content);
            userMentionIds = List.copyOf(userMentionIds == null ? List.of() : userMentionIds);
            roleMentionIds = List.copyOf(roleMentionIds == null ? List.of() : roleMentionIds);
        }
    }

    /** Lightweight reaction emoji snapshot. */
    public record EmojiSnapshot(String id, String name, boolean animated, String raw) {
        public EmojiSnapshot {
            id = nullToEmpty(id);
            name = nullToEmpty(name);
            raw = nullToEmpty(raw);
        }
    }

    /** Published when Discord reports a guild create/available event. */
    public record GuildCreated(GuildSnapshot guild, Instant occurredAt) implements BotEvent {
        public GuildCreated { Objects.requireNonNull(guild, "guild"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when Discord reports a guild update event. */
    public record GuildUpdated(GuildSnapshot current, GuildSnapshot previous, Instant occurredAt) implements BotEvent {
        public GuildUpdated { Objects.requireNonNull(current, "current"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when Discord reports a guild delete/unavailable event. */
    public record GuildDeleted(String guildId, GuildSnapshot cachedGuild, boolean unavailable, Instant occurredAt) implements BotEvent {
        public GuildDeleted { guildId = requireText(guildId, "guildId"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a guild channel is created. */
    public record ChannelCreated(ChannelSnapshot channel, Instant occurredAt) implements BotEvent {
        public ChannelCreated { Objects.requireNonNull(channel, "channel"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a guild channel is updated. */
    public record ChannelUpdated(ChannelSnapshot current, ChannelSnapshot previous, Instant occurredAt) implements BotEvent {
        public ChannelUpdated { Objects.requireNonNull(current, "current"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a guild channel is deleted. */
    public record ChannelDeleted(ChannelSnapshot channel, Instant occurredAt) implements BotEvent {
        public ChannelDeleted { Objects.requireNonNull(channel, "channel"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when channel pins change. */
    public record PinsUpdated(String channelId, String guildId, Instant lastPinTimestamp, Instant occurredAt) implements BotEvent {
        public PinsUpdated { channelId = requireText(channelId, "channelId"); guildId = nullToEmpty(guildId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a user starts typing. */
    public record TypingStarted(String channelId, String guildId, String userId, MemberSnapshot member, Instant startedAt, Instant occurredAt) implements BotEvent {
        public TypingStarted { channelId = requireText(channelId, "channelId"); guildId = nullToEmpty(guildId); userId = requireText(userId, "userId"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a message is created. */
    public record MessageCreated(MessageSnapshot message, MemberSnapshot member, Instant occurredAt) implements BotEvent {
        public MessageCreated { Objects.requireNonNull(message, "message"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a message is updated. */
    public record MessageUpdated(String messageId, String channelId, String guildId, String currentContent, boolean contentChanged, boolean embedsChanged, MessageSnapshot cachedPrevious, Instant occurredAt) implements BotEvent {
        public MessageUpdated { messageId = requireText(messageId, "messageId"); channelId = requireText(channelId, "channelId"); guildId = nullToEmpty(guildId); currentContent = nullToEmpty(currentContent); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a message is deleted. */
    public record MessageDeleted(String messageId, String channelId, String guildId, MessageSnapshot cachedMessage, Instant occurredAt) implements BotEvent {
        public MessageDeleted { messageId = requireText(messageId, "messageId"); channelId = requireText(channelId, "channelId"); guildId = nullToEmpty(guildId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when multiple messages are deleted. */
    public record MessageBulkDeleted(Set<String> messageIds, String channelId, String guildId, Instant occurredAt) implements BotEvent {
        public MessageBulkDeleted { messageIds = Set.copyOf(messageIds == null ? Set.of() : messageIds); channelId = requireText(channelId, "channelId"); guildId = requireText(guildId, "guildId"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a reaction is added to a message. */
    public record ReactionAdded(String userId, String channelId, String messageId, String guildId, EmojiSnapshot emoji, MemberSnapshot member, String messageAuthorId, Instant occurredAt) implements BotEvent {
        public ReactionAdded { userId = requireText(userId, "userId"); channelId = requireText(channelId, "channelId"); messageId = requireText(messageId, "messageId"); guildId = nullToEmpty(guildId); Objects.requireNonNull(emoji, "emoji"); messageAuthorId = nullToEmpty(messageAuthorId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a reaction is removed from a message. */
    public record ReactionRemoved(String userId, String channelId, String messageId, String guildId, EmojiSnapshot emoji, Instant occurredAt) implements BotEvent {
        public ReactionRemoved { userId = requireText(userId, "userId"); channelId = requireText(channelId, "channelId"); messageId = requireText(messageId, "messageId"); guildId = nullToEmpty(guildId); Objects.requireNonNull(emoji, "emoji"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when all reactions are removed from a message. */
    public record ReactionsCleared(String channelId, String messageId, String guildId, Instant occurredAt) implements BotEvent {
        public ReactionsCleared { channelId = requireText(channelId, "channelId"); messageId = requireText(messageId, "messageId"); guildId = nullToEmpty(guildId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when all reactions for one emoji are removed from a message. */
    public record ReactionEmojiCleared(String channelId, String messageId, String guildId, EmojiSnapshot emoji, Instant occurredAt) implements BotEvent {
        public ReactionEmojiCleared { channelId = requireText(channelId, "channelId"); messageId = requireText(messageId, "messageId"); guildId = nullToEmpty(guildId); Objects.requireNonNull(emoji, "emoji"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a member joins a guild. */
    public record MemberJoined(MemberSnapshot member, Instant occurredAt) implements BotEvent {
        public MemberJoined { Objects.requireNonNull(member, "member"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a member leaves a guild. */
    public record MemberLeft(String guildId, UserSnapshot user, MemberSnapshot cachedMember, Instant occurredAt) implements BotEvent {
        public MemberLeft { guildId = requireText(guildId, "guildId"); Objects.requireNonNull(user, "user"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a member is updated. */
    public record MemberUpdated(String guildId, String memberId, Set<String> currentRoleIds, String currentNickname, Instant joinedAt, boolean pending, MemberSnapshot cachedPrevious, Instant occurredAt) implements BotEvent {
        public MemberUpdated { guildId = requireText(guildId, "guildId"); memberId = requireText(memberId, "memberId"); currentRoleIds = Set.copyOf(currentRoleIds == null ? Set.of() : currentRoleIds); currentNickname = nullToEmpty(currentNickname); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a role is created. */
    public record RoleCreated(RoleSnapshot role, Instant occurredAt) implements BotEvent {
        public RoleCreated { Objects.requireNonNull(role, "role"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a role is updated. */
    public record RoleUpdated(RoleSnapshot current, RoleSnapshot previous, Instant occurredAt) implements BotEvent {
        public RoleUpdated { Objects.requireNonNull(current, "current"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a role is deleted. */
    public record RoleDeleted(String guildId, String roleId, RoleSnapshot cachedRole, Instant occurredAt) implements BotEvent {
        public RoleDeleted { guildId = requireText(guildId, "guildId"); roleId = requireText(roleId, "roleId"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a user's profile is updated. */
    public record UserUpdated(UserSnapshot current, UserSnapshot previous, Instant occurredAt) implements BotEvent {
        public UserUpdated { Objects.requireNonNull(current, "current"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a guild presence update is received. */
    public record PresenceUpdated(String guildId, String userId, String status, UserSnapshot oldUser, Instant occurredAt) implements BotEvent {
        public PresenceUpdated { guildId = requireText(guildId, "guildId"); userId = requireText(userId, "userId"); status = nullToEmpty(status); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when a voice state changes. */
    public record VoiceStateUpdated(String guildId, String userId, String channelId, boolean join, boolean leave, boolean move, Instant occurredAt) implements BotEvent {
        public VoiceStateUpdated { guildId = nullToEmpty(guildId); userId = requireText(userId, "userId"); channelId = nullToEmpty(channelId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when an invite is created. */
    public record InviteCreated(String code, String channelId, String guildId, UserSnapshot inviter, int uses, int maxUses, int maxAgeSeconds, boolean temporary, Instant expiresAt, Instant occurredAt) implements BotEvent {
        public InviteCreated { code = requireText(code, "code"); channelId = requireText(channelId, "channelId"); guildId = nullToEmpty(guildId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when an invite is deleted. */
    public record InviteDeleted(String code, String channelId, String guildId, Instant occurredAt) implements BotEvent {
        public InviteDeleted { code = requireText(code, "code"); channelId = requireText(channelId, "channelId"); guildId = nullToEmpty(guildId); occurredAt = defaultNow(occurredAt); }
    }

    /** Published when webhooks change in a channel. */
    public record WebhooksUpdated(String guildId, String channelId, Instant occurredAt) implements BotEvent {
        public WebhooksUpdated { guildId = requireText(guildId, "guildId"); channelId = requireText(channelId, "channelId"); occurredAt = defaultNow(occurredAt); }
    }

    /** Published for any Discord interaction, including slash commands, buttons, selects, modals, and autocomplete. */
    public record InteractionReceived(String interactionId, String applicationId, String channelId, String guildId, String userId, String interactionType, Instant occurredAt) implements BotEvent {
        public InteractionReceived { interactionId = requireText(interactionId, "interactionId"); applicationId = nullToEmpty(applicationId); channelId = nullToEmpty(channelId); guildId = nullToEmpty(guildId); userId = nullToEmpty(userId); interactionType = nullToEmpty(interactionType); occurredAt = defaultNow(occurredAt); }
    }

    private static Instant defaultNow(Instant occurredAt) {
        return occurredAt == null ? Instant.now() : occurredAt;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

