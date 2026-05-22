package io.github.yvancywan.anvilcord.core.event;

import module java.base;

/**
 * Immutable user-profile snapshot for the authenticated bot account.
 */
public record BotUserProfile(
        String id,
        String username,
        String discriminator,
        String tag,
        String avatarUrl,
        boolean bot
) {

    public BotUserProfile {
        id = requireNonBlank(id, "id");
        username = requireNonBlank(username, "username");
        discriminator = discriminator == null ? "" : discriminator;
        tag = tag == null || tag.isBlank() ? username : tag;
        avatarUrl = avatarUrl == null ? "" : avatarUrl;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Bot user profile " + fieldName + " must not be blank");
        }
        return value;
    }
}

