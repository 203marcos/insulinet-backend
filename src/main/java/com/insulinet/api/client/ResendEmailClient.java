package com.insulinet.api.client;

import com.insulinet.api.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP minimo para a API do Resend (POST /emails). Implementado com
 * RestClient (padrao Spring, ja disponivel via spring-boot-starter-web) em
 * vez do SDK Java oficial do Resend, para nao adicionar mais uma dependencia
 * externa so por causa de uma unica chamada HTTP simples.
 */
@Component
public class ResendEmailClient {

    private final RestClient resendRestClient;
    private final AppProperties appProperties;

    public ResendEmailClient(RestClient resendRestClient, AppProperties appProperties) {
        this.resendRestClient = resendRestClient;
        this.appProperties = appProperties;
    }

    public void sendEmail(String recipientEmail, String subject, String html) {
        Map<String, Object> payload = Map.of(
                "from", appProperties.email().from(),
                "to", List.of(recipientEmail),
                "subject", subject,
                "html", html
        );

        resendRestClient.post()
                .uri("/emails")
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
