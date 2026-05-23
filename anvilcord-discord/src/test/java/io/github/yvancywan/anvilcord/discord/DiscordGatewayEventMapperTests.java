package io.github.yvancywan.anvilcord.discord;

import io.github.yvancywan.anvilcord.core.event.BotEvent;
import io.github.yvancywan.anvilcord.discord.event.DiscordBotActions;
import io.github.yvancywan.anvilcord.discord.event.DiscordGatewayEvents;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class DiscordGatewayEventMapperTests {

    @Test
    void mapsMessageCreateIntoStablePayload() {
        Instant occurredAt = Instant.parse("2026-05-23T12:00:00Z");
        User author = mock(User.class);
        when(author.getId()).thenReturn(Snowflake.of(42L));
        when(author.getUsername()).thenReturn("announcer");
        when(author.getTag()).thenReturn("announcer#0001");
        when(author.getGlobalName()).thenReturn(Optional.of("Announcement Bot"));
        when(author.getAvatarUrl()).thenReturn("https://cdn.example/avatar.png");
        when(author.isBot()).thenReturn(true);

        Message message = mock(Message.class);
        when(message.getId()).thenReturn(Snowflake.of(100L));
        when(message.getChannelId()).thenReturn(Snowflake.of(200L));
        when(message.getGuildId()).thenReturn(Optional.of(Snowflake.of(300L)));
        when(message.getAuthor()).thenReturn(Optional.of(author));
        when(message.getContent()).thenReturn("Release deployed");
        when(message.getTimestamp()).thenReturn(occurredAt.minusSeconds(1));
        when(message.getEditedTimestamp()).thenReturn(Optional.empty());
        when(message.isPinned()).thenReturn(false);
        when(message.isTts()).thenReturn(false);
        when(message.getUserMentionIds()).thenReturn(List.of(Snowflake.of(55L)));
        when(message.getRoleMentionIds()).thenReturn(List.of(Snowflake.of(66L)));

        MessageCreateEvent event = mock(MessageCreateEvent.class);
        when(event.getMessage()).thenReturn(message);
        when(event.getMember()).thenReturn(Optional.empty());

        List<BotEvent> mapped = DiscordGatewayEventMapper.map(event, occurredAt);

        assertThat(mapped).hasSize(1);
        assertThat(mapped.getFirst()).isInstanceOf(DiscordGatewayEvents.MessageCreated.class);
        DiscordGatewayEvents.MessageCreated created = (DiscordGatewayEvents.MessageCreated) mapped.getFirst();
        assertThat(created.occurredAt()).isEqualTo(occurredAt);
        assertThat(created.message().id()).isEqualTo("100");
        assertThat(created.message().channelId()).isEqualTo("200");
        assertThat(created.message().guildId()).isEqualTo("300");
        assertThat(created.message().content()).isEqualTo("Release deployed");
        assertThat(created.message().author().username()).isEqualTo("announcer");
        assertThat(created.message().userMentionIds()).containsExactly("55");
        assertThat(created.message().roleMentionIds()).containsExactly("66");
    }

    @Test
    void actionPayloadsRequireAddressableTargetsAndContent() {
        assertThatThrownBy(() -> new DiscordBotActions.SendChannelMessage("", "hello", "corr", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("channelId");
        assertThatThrownBy(() -> new DiscordBotActions.SendChannelMessage("123", " ", "corr", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content");

        DiscordBotActions.SendChannelMessage action = new DiscordBotActions.SendChannelMessage("123", "hello", "", null);

        assertThat(action.correlationId()).isNotBlank();
        assertThat(action.occurredAt()).isNotNull();
    }
}



