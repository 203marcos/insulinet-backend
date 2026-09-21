package com.insulinet.api.service;

import com.insulinet.api.config.AppProperties;
import com.insulinet.api.exception.BadRequestException;
import com.insulinet.api.exception.ConflictException;
import com.insulinet.api.exception.UnauthorizedException;
import com.insulinet.api.mapper.UserMapper;
import com.insulinet.api.model.dto.auth.ForgotPasswordResponse;
import com.insulinet.api.model.dto.auth.RegisterRequest;
import com.insulinet.api.model.dto.auth.ResetPasswordResponse;
import com.insulinet.api.model.dto.auth.TokenResponse;
import com.insulinet.api.model.dto.auth.UserResponse;
import com.insulinet.api.model.entity.PasswordResetToken;
import com.insulinet.api.model.entity.User;
import com.insulinet.api.repository.PasswordResetTokenRepository;
import com.insulinet.api.repository.UserRepository;
import com.insulinet.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Espelha app/api/routes/auth.py (que no Python continha a regra de negocio
 * diretamente nas rotas) e app/core/security.py.
 */
@Service
public class AuthService {

    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "Se existir uma conta associada a este e-mail, as instrucoes de recuperacao serao enviadas.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            UserMapper userMapper,
            AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.userMapper = userMapper;
        this.appProperties = appProperties;
    }

    public TokenResponse login(String rawUsername, String password) {
        String email = normalizeEmail(rawUsername);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("E-mail ou senha invalidos.");
        }

        return new TokenResponse(jwtService.generateToken(user.getId()), "bearer");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ja existe uma conta com este e-mail.");
        }

        User user = new User();
        user.setName(request.name().strip());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(Instant.now());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return new ForgotPasswordResponse(GENERIC_FORGOT_PASSWORD_MESSAGE);
        }

        String rawToken = generatePasswordResetToken();
        String tokenHash = hashPasswordResetToken(rawToken);
        Instant expiresAt = Instant.now()
                .plus(appProperties.passwordReset().expireMinutes(), ChronoUnit.MINUTES);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setCreatedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        emailService.trySendPasswordResetEmail(user.getEmail(), rawToken);

        return new ForgotPasswordResponse(GENERIC_FORGOT_PASSWORD_MESSAGE);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(String token, String newPassword) {
        String tokenHash = hashPasswordResetToken(token);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Token invalido ou expirado."));

        Instant now = Instant.now();

        if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("Token invalido ou expirado.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsedAt(now);

        return new ResetPasswordResponse("Senha alterada com sucesso.");
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private String generatePasswordResetToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashPasswordResetToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel na JVM.", e);
        }
    }
}
