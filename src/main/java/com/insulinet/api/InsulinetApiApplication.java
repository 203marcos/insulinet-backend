package com.insulinet.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// UserDetailsServiceAutoConfiguration e excluida porque a autenticacao e
// feita inteiramente via JWT (JwtAuthenticationFilter monta o Authentication
// manualmente); sem essa exclusao o Spring Boot gera e loga um usuario/senha
// em memoria que nunca e usado.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class InsulinetApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsulinetApiApplication.class, args);
    }
}
