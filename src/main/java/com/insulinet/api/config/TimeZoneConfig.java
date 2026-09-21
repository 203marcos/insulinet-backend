package com.insulinet.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class TimeZoneConfig {

    @Bean
    public ZoneId appTimeZone(AppProperties appProperties) {
        return ZoneId.of(appProperties.timezone());
    }
}
