package com.insulinet.api.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 1, max = 100) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
