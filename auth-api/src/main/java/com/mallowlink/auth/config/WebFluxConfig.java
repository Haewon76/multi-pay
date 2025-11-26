package com.mallowlink.auth.config;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFluxConfig {

    @Bean
    public WebProperties webProperties() {
        return new WebProperties();
    }
}