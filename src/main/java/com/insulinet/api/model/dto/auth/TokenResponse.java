package com.insulinet.api.model.dto.auth;

public record TokenResponse(
        String accessToken,
        String tokenType
) {
}
