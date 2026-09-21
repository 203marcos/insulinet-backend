package com.insulinet.api.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * O backend Python (Pydantic v2) serializa campos Decimal como string JSON
 * (ex.: "12.50"), preservando a escala exata vinda do NUMERIC(10,2) do
 * banco - nao como numero. Para o frontend continuar recebendo o mesmo
 * formato de payload, replicamos esse comportamento para BigDecimal aqui,
 * globalmente, em vez de anotar campo a campo em cada DTO.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }
}
