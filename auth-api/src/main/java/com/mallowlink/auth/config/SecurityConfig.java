package com.mallowlink.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static com.mallowlink.auth.config.TokenValidationConfig.AUTHORIZED_PATHS;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // 인증이 필요 없는 경로 패턴들을 상수로 정의


    // 필요에 따라 추가 카테고리 정의 가능
    public static final String[] ADMIN_ONLY_PATHS = {
            // 관리자 전용 경로
    };



    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuring security web filter chain");

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(AUTHORIZED_PATHS).permitAll()
                .anyExchange().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}
