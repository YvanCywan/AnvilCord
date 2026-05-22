package com.yvan.cywan.anvilcord.core.config;

import module java.base;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External configuration for the framework-owned Discord connection.
 *
 * <p>The framework intentionally keeps these values simple strings at the
 * boundary so local development can leave them blank without failing Spring's
 * configuration binder. Callers can then explicitly decide when a token or
 * application id is required.</p>
 */
@ConfigurationProperties(prefix = "bot.core")
public record BotCoreProperties(String token, String applicationId) {

    /**
     * Normalize nullable configuration values before the record instance exists.
     */
    public BotCoreProperties {
        token = token == null ? "" : token.strip();
        applicationId = applicationId == null ? "" : applicationId.strip();
    }

    /**
     * @return {@code true} when a Discord bot token has been provided.
     */
    public boolean hasToken() {
        return !token.isBlank();
    }

    /**
     * Returns the configured token or fails with an actionable message.
     */
    public String requireToken() {
        if (!hasToken()) {
            throw new IllegalStateException("bot.core.token must be configured before connecting to Discord");
        }
        return token;
    }

    /**
     * @return the configured Discord application id, if present and numeric.
     */
    public OptionalLong applicationIdAsLong() {
        if (applicationId.isBlank()) {
            return OptionalLong.empty();
        }

        try {
            return OptionalLong.of(Long.parseLong(applicationId));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("bot.core.application-id must be a numeric Discord snowflake", exception);
        }
    }
}

