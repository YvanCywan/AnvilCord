package io.github.yvancywan.anvilcord.discord;

import io.github.yvancywan.anvilcord.core.event.BotEvent;
import io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvents;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.InviteCreateEvent;
import discord4j.core.event.domain.InviteDeleteEvent;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.UserUpdateEvent;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.WebhooksUpdateEvent;
import discord4j.core.event.domain.channel.PinsUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.TextChannelUpdateEvent;
import discord4j.core.event.domain.channel.TypingStartEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.GuildUpdateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveAllEvent;
import discord4j.core.event.domain.message.ReactionRemoveEmojiEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.reaction.ReactionEmoji;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Maps Discord4J gateway events into public AnvilCord payloads. */
final class DiscordGatewayEventMapper {

    private DiscordGatewayEventMapper() {
    }

    static List<BotEvent> map(Event event, Instant occurredAt) {
        List<BotEvent> mapped = new ArrayList<>();

        if (event instanceof MessageCreateEvent messageCreateEvent) {
            mapped.add(new DiscordGatewayEvents.MessageCreated(message(messageCreateEvent.getMessage()), messageCreateEvent.getMember().map(DiscordGatewayEventMapper::member).orElse(null), occurredAt));
        } else if (event instanceof MessageUpdateEvent messageUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.MessageUpdated(id(messageUpdateEvent.getMessageId()), id(messageUpdateEvent.getChannelId()), optionalId(messageUpdateEvent.getGuildId()), messageUpdateEvent.getCurrentContent().orElse(""), messageUpdateEvent.isContentChanged(), messageUpdateEvent.isEmbedsChanged(), messageUpdateEvent.getOld().map(DiscordGatewayEventMapper::message).orElse(null), occurredAt));
        } else if (event instanceof MessageDeleteEvent messageDeleteEvent) {
            mapped.add(new DiscordGatewayEvents.MessageDeleted(id(messageDeleteEvent.getMessageId()), id(messageDeleteEvent.getChannelId()), optionalId(messageDeleteEvent.getGuildId()), messageDeleteEvent.getMessage().map(DiscordGatewayEventMapper::message).orElse(null), occurredAt));
        } else if (event instanceof MessageBulkDeleteEvent bulkDeleteEvent) {
            mapped.add(new DiscordGatewayEvents.MessageBulkDeleted(ids(bulkDeleteEvent.getMessageIds()), id(bulkDeleteEvent.getChannelId()), id(bulkDeleteEvent.getGuildId()), occurredAt));
        } else if (event instanceof ReactionAddEvent reactionAddEvent) {
            mapped.add(new DiscordGatewayEvents.ReactionAdded(id(reactionAddEvent.getUserId()), id(reactionAddEvent.getChannelId()), id(reactionAddEvent.getMessageId()), optionalId(reactionAddEvent.getGuildId()), emoji(reactionAddEvent.getEmoji()), reactionAddEvent.getMember().map(DiscordGatewayEventMapper::member).orElse(null), id(reactionAddEvent.getMessageAuthorId()), occurredAt));
        } else if (event instanceof ReactionRemoveEvent reactionRemoveEvent) {
            mapped.add(new DiscordGatewayEvents.ReactionRemoved(id(reactionRemoveEvent.getUserId()), id(reactionRemoveEvent.getChannelId()), id(reactionRemoveEvent.getMessageId()), optionalId(reactionRemoveEvent.getGuildId()), emoji(reactionRemoveEvent.getEmoji()), occurredAt));
        } else if (event instanceof ReactionRemoveAllEvent reactionRemoveAllEvent) {
            mapped.add(new DiscordGatewayEvents.ReactionsCleared(id(reactionRemoveAllEvent.getChannelId()), id(reactionRemoveAllEvent.getMessageId()), optionalId(reactionRemoveAllEvent.getGuildId()), occurredAt));
        } else if (event instanceof ReactionRemoveEmojiEvent reactionRemoveEmojiEvent) {
            mapped.add(new DiscordGatewayEvents.ReactionEmojiCleared(id(reactionRemoveEmojiEvent.getChannelId()), id(reactionRemoveEmojiEvent.getMessageId()), optionalId(reactionRemoveEmojiEvent.getGuildId()), emoji(reactionRemoveEmojiEvent.getEmoji()), occurredAt));
        }

        if (event instanceof GuildCreateEvent guildCreateEvent) {
            mapped.add(new DiscordGatewayEvents.GuildCreated(guild(guildCreateEvent.getGuild()), occurredAt));
        } else if (event instanceof GuildUpdateEvent guildUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.GuildUpdated(guild(guildUpdateEvent.getCurrent()), guildUpdateEvent.getOld().map(DiscordGatewayEventMapper::guild).orElse(null), occurredAt));
        } else if (event instanceof GuildDeleteEvent guildDeleteEvent) {
            mapped.add(new DiscordGatewayEvents.GuildDeleted(id(guildDeleteEvent.getGuildId()), guildDeleteEvent.getGuild().map(DiscordGatewayEventMapper::guild).orElse(null), guildDeleteEvent.isUnavailable(), occurredAt));
        } else if (event instanceof MemberJoinEvent memberJoinEvent) {
            mapped.add(new DiscordGatewayEvents.MemberJoined(member(memberJoinEvent.getMember()), occurredAt));
        } else if (event instanceof MemberLeaveEvent memberLeaveEvent) {
            mapped.add(new DiscordGatewayEvents.MemberLeft(id(memberLeaveEvent.getGuildId()), user(memberLeaveEvent.getUser()), memberLeaveEvent.getMember().map(DiscordGatewayEventMapper::member).orElse(null), occurredAt));
        } else if (event instanceof MemberUpdateEvent memberUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.MemberUpdated(id(memberUpdateEvent.getGuildId()), id(memberUpdateEvent.getMemberId()), ids(memberUpdateEvent.getCurrentRoleIds()), memberUpdateEvent.getCurrentNickname().orElse(""), memberUpdateEvent.getJoinTime().orElse(null), memberUpdateEvent.isCurrentPending(), memberUpdateEvent.getOld().map(DiscordGatewayEventMapper::member).orElse(null), occurredAt));
        }

        if (event instanceof TextChannelCreateEvent channelCreateEvent) {
            mapped.add(new DiscordGatewayEvents.ChannelCreated(channel(channelCreateEvent.getChannel()), occurredAt));
        } else if (event instanceof TextChannelUpdateEvent channelUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.ChannelUpdated(channel(channelUpdateEvent.getCurrent()), channelUpdateEvent.getOld().map(DiscordGatewayEventMapper::channel).orElse(null), occurredAt));
        } else if (event instanceof TextChannelDeleteEvent channelDeleteEvent) {
            mapped.add(new DiscordGatewayEvents.ChannelDeleted(channel(channelDeleteEvent.getChannel()), occurredAt));
        } else if (isChannelMutation(event, "Create")) {
            reflectedChannel(event, "getChannel").ifPresent(value -> mapped.add(new DiscordGatewayEvents.ChannelCreated(channel(value), occurredAt)));
        } else if (isChannelMutation(event, "Update")) {
            reflectedChannel(event, "getCurrent").ifPresent(value -> mapped.add(new DiscordGatewayEvents.ChannelUpdated(channel(value), reflectedOldChannel(event).map(DiscordGatewayEventMapper::channel).orElse(null), occurredAt)));
        } else if (isChannelMutation(event, "Delete")) {
            reflectedChannel(event, "getChannel").ifPresent(value -> mapped.add(new DiscordGatewayEvents.ChannelDeleted(channel(value), occurredAt)));
        } else if (event instanceof PinsUpdateEvent pinsUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.PinsUpdated(id(pinsUpdateEvent.getChannelId()), optionalId(pinsUpdateEvent.getGuildId()), pinsUpdateEvent.getLastPinTimestamp().orElse(null), occurredAt));
        } else if (event instanceof TypingStartEvent typingStartEvent) {
            mapped.add(new DiscordGatewayEvents.TypingStarted(id(typingStartEvent.getChannelId()), optionalId(typingStartEvent.getGuildId()), id(typingStartEvent.getUserId()), typingStartEvent.getMember().map(DiscordGatewayEventMapper::member).orElse(null), typingStartEvent.getStartTime(), occurredAt));
        }

        if (event instanceof RoleCreateEvent roleCreateEvent) {
            mapped.add(new DiscordGatewayEvents.RoleCreated(role(roleCreateEvent.getRole()), occurredAt));
        } else if (event instanceof RoleUpdateEvent roleUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.RoleUpdated(role(roleUpdateEvent.getCurrent()), roleUpdateEvent.getOld().map(DiscordGatewayEventMapper::role).orElse(null), occurredAt));
        } else if (event instanceof RoleDeleteEvent roleDeleteEvent) {
            mapped.add(new DiscordGatewayEvents.RoleDeleted(id(roleDeleteEvent.getGuildId()), id(roleDeleteEvent.getRoleId()), roleDeleteEvent.getRole().map(DiscordGatewayEventMapper::role).orElse(null), occurredAt));
        }

        if (event instanceof UserUpdateEvent userUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.UserUpdated(user(userUpdateEvent.getCurrent()), userUpdateEvent.getOld().map(DiscordGatewayEventMapper::user).orElse(null), occurredAt));
        } else if (event instanceof PresenceUpdateEvent presenceUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.PresenceUpdated(id(presenceUpdateEvent.getGuildId()), id(presenceUpdateEvent.getUserId()), String.valueOf(presenceUpdateEvent.getCurrent().getStatus()), presenceUpdateEvent.getOldUser().map(DiscordGatewayEventMapper::user).orElse(null), occurredAt));
        } else if (event instanceof VoiceStateUpdateEvent voiceStateUpdateEvent) {
            VoiceState current = voiceStateUpdateEvent.getCurrent();
            mapped.add(new DiscordGatewayEvents.VoiceStateUpdated(id(current.getGuildId()), id(current.getUserId()), optionalId(current.getChannelId()), voiceStateUpdateEvent.isJoinEvent(), voiceStateUpdateEvent.isLeaveEvent(), voiceStateUpdateEvent.isMoveEvent(), occurredAt));
        } else if (event instanceof InviteCreateEvent inviteCreateEvent) {
            mapped.add(new DiscordGatewayEvents.InviteCreated(inviteCreateEvent.getCode(), id(inviteCreateEvent.getChannelId()), optionalId(inviteCreateEvent.getGuildId()), inviteCreateEvent.getInviter().map(DiscordGatewayEventMapper::user).orElse(null), inviteCreateEvent.getUses(), inviteCreateEvent.getMaxUses(), inviteCreateEvent.getMaxAge(), inviteCreateEvent.isTemporary(), inviteCreateEvent.getExpiration().orElse(null), occurredAt));
        } else if (event instanceof InviteDeleteEvent inviteDeleteEvent) {
            mapped.add(new DiscordGatewayEvents.InviteDeleted(inviteDeleteEvent.getCode(), id(inviteDeleteEvent.getChannelId()), optionalId(inviteDeleteEvent.getGuildId()), occurredAt));
        } else if (event instanceof WebhooksUpdateEvent webhooksUpdateEvent) {
            mapped.add(new DiscordGatewayEvents.WebhooksUpdated(id(webhooksUpdateEvent.getGuildId()), id(webhooksUpdateEvent.getChannelId()), occurredAt));
        } else if (event instanceof InteractionCreateEvent interactionCreateEvent) {
            mapped.add(interaction(interactionCreateEvent.getInteraction(), occurredAt));
        }

        return List.copyOf(mapped);
    }

    private static DiscordGatewayEvents.UserSnapshot user(User user) {
        return new DiscordGatewayEvents.UserSnapshot(id(user.getId()), user.getUsername(), "", user.getTag(), user.getGlobalName().orElse(""), user.getAvatarUrl(), user.isBot());
    }

    private static DiscordGatewayEvents.MemberSnapshot member(Member member) {
        return new DiscordGatewayEvents.MemberSnapshot(user(member), id(member.getGuildId()), member.getDisplayName(), member.getNickname().orElse(""), ids(member.getRoleIds()), member.getJoinTime().orElse(null));
    }

    private static DiscordGatewayEvents.GuildSnapshot guild(Guild guild) {
        return new DiscordGatewayEvents.GuildSnapshot(id(guild.getId()), guild.getName(), id(guild.getOwnerId()), guild.getMemberCount(), guild.isUnavailable());
    }

    private static DiscordGatewayEvents.ChannelSnapshot channel(Channel channel) {
        String guildId = channel instanceof GuildChannel guildChannel ? id(guildChannel.getGuildId()) : "";
        String name = channel instanceof GuildChannel guildChannel ? guildChannel.getName() : "";
        return new DiscordGatewayEvents.ChannelSnapshot(id(channel.getId()), guildId, name, String.valueOf(channel.getType()));
    }

    private static DiscordGatewayEvents.RoleSnapshot role(Role role) {
        return new DiscordGatewayEvents.RoleSnapshot(id(role.getId()), id(role.getGuildId()), role.getName(), role.getRawPosition(), String.valueOf(role.getPermissions()), role.isManaged(), role.isMentionable());
    }

    private static DiscordGatewayEvents.MessageSnapshot message(Message message) {
        return new DiscordGatewayEvents.MessageSnapshot(
                id(message.getId()),
                id(message.getChannelId()),
                optionalId(message.getGuildId()),
                message.getAuthor().map(DiscordGatewayEventMapper::user).orElse(null),
                message.getContent(),
                message.getTimestamp(),
                message.getEditedTimestamp().orElse(null),
                message.isPinned(),
                message.isTts(),
                listIds(message.getUserMentionIds()),
                listIds(message.getRoleMentionIds())
        );
    }

    private static DiscordGatewayEvents.EmojiSnapshot emoji(ReactionEmoji emoji) {
        Optional<ReactionEmoji.Custom> custom = emoji.asCustomEmoji();
        if (custom.isPresent()) {
            ReactionEmoji.Custom value = custom.get();
            return new DiscordGatewayEvents.EmojiSnapshot(id(value.getId()), value.getName(), value.isAnimated(), value.asFormat());
        }
        return new DiscordGatewayEvents.EmojiSnapshot("", emoji.asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw).orElse(""), false, emoji.toString());
    }

    private static DiscordGatewayEvents.InteractionReceived interaction(Interaction interaction, Instant occurredAt) {
        return new DiscordGatewayEvents.InteractionReceived(
                id(interaction.getId()),
                id(interaction.getApplicationId()),
                id(interaction.getChannelId()),
                optionalId(interaction.getGuildId()),
                id(interaction.getUser().getId()),
                String.valueOf(interaction.getType()),
                occurredAt
        );
    }

    private static Set<String> ids(Set<Snowflake> snowflakes) {
        return snowflakes.stream().map(DiscordGatewayEventMapper::id).collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isChannelMutation(Event event, String verb) {
        return event.getClass().getPackageName().equals("discord4j.core.event.domain.channel")
                && event.getClass().getSimpleName().endsWith("Channel" + verb + "Event");
    }

    private static Optional<Channel> reflectedChannel(Event event, String accessor) {
        try {
            Object value = event.getClass().getMethod(accessor).invoke(event);
            return value instanceof Channel channel ? Optional.of(channel) : Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<Channel> reflectedOldChannel(Event event) {
        try {
            Object value = event.getClass().getMethod("getOld").invoke(event);
            if (value instanceof Optional<?> optional && optional.orElse(null) instanceof Channel channel) {
                return Optional.of(channel);
            }
            return Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private static List<String> listIds(List<Snowflake> snowflakes) {
        return snowflakes.stream().map(DiscordGatewayEventMapper::id).toList();
    }

    private static String optionalId(Optional<Snowflake> snowflake) {
        return snowflake.map(DiscordGatewayEventMapper::id).orElse("");
    }

    private static String id(Snowflake snowflake) {
        return snowflake.asString();
    }
}




