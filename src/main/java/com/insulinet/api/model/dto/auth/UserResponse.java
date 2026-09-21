package com.insulinet.api.model.dto.auth;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Instant createdAt
) {
}
