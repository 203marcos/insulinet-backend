package com.insulinet.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        String frontendUrl,
        Cors cors,
        Jwt jwt,
        PasswordReset passwordReset,
        Email email
) {
    public record Cors(String allowedOrigins) {
    }

    public record Jwt(String secretKey, long accessTokenExpireMinutes) {
    }

    public record PasswordReset(long expireMinutes) {
    }

    public record Email(String resendApiKey, String from) {
    }
}
