package com.insulinet.api.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(min = 20, max = 500) String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
