package com.insulinet.api.controller;

import com.insulinet.api.model.dto.auth.ForgotPasswordRequest;
import com.insulinet.api.model.dto.auth.ForgotPasswordResponse;
import com.insulinet.api.model.dto.auth.RegisterRequest;
import com.insulinet.api.model.dto.auth.ResetPasswordRequest;
import com.insulinet.api.model.dto.auth.ResetPasswordResponse;
import com.insulinet.api.model.dto.auth.TokenResponse;
import com.insulinet.api.model.dto.auth.UserResponse;
import com.insulinet.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Compativel com o fluxo OAuth2PasswordBearer do FastAPI: o frontend
     * envia application/x-www-form-urlencoded com os campos username/password.
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse login(
            @RequestParam String username,
            @RequestParam String password
    ) {
        return authService.login(username, password);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    public ResetPasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request.token(), request.newPassword());
    }
}
