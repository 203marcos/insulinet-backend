package com.insulinet.api.service;

import com.insulinet.api.client.ResendEmailClient;
import com.insulinet.api.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Espelha app/services/email_service.py.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ResendEmailClient resendEmailClient;
    private final AppProperties appProperties;

    public EmailService(ResendEmailClient resendEmailClient, AppProperties appProperties) {
        this.resendEmailClient = resendEmailClient;
        this.appProperties = appProperties;
    }

    /**
     * Falha de envio e sempre capturada e logada aqui, nunca propagada: o
     * endpoint de forgot-password sempre responde com a mensagem generica,
     * independentemente do e-mail ter sido enviado com sucesso.
     */
    public void trySendPasswordResetEmail(String recipientEmail, String token) {
        try {
            String resetUrl = UriComponentsBuilder
                    .fromUriString(stripTrailingSlash(appProperties.frontendUrl()) + "/reset-password")
                    .queryParam("token", token)
                    .build()
                    .toUriString();

            String html = """
                    <div>
                        <h2>Recuperacao de senha</h2>
                        <p>Recebemos uma solicitacao para alterar a senha da sua conta.</p>
                        <p><a href="%s">Alterar minha senha</a></p>
                        <p>Este link expira em 30 minutos.</p>
                    </div>
                    """.formatted(resetUrl);

            resendEmailClient.sendEmail(recipientEmail, "Recuperacao de senha - Insulinet", html);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de recuperacao de senha.", e);
        }
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
