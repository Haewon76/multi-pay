package com.mallowlink.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Arrays;
import java.util.List;

import static com.mallowlink.gateway.config.TokenValidationConfig.AUTHORIZED_PATHS;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    public static final String AUTHORIZATION_PREFIX = "/api/auth";

    // 인증이 필요 없는 경로 패턴들을 상수로 정의
    /*public static final List<String> AUTHORIZED_PATHS = List.of(
            "/**",
            "/client/get/**",
            "/client/exists/**",
            "/devOffice/client/create/**",
            "/devOffice/client/update/**",
            "/devOffice/client/delete/**"
    );*/

    public static String[] getPublicPaths() {
        return Arrays.stream(AUTHORIZED_PATHS)
                .map(path -> AUTHORIZATION_PREFIX + path)
                .toArray(String[]::new);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuring gateway security web filter chain");

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(getPublicPaths()).permitAll()
                .anyExchange().authenticated()
            )
            .build();
    }

}
