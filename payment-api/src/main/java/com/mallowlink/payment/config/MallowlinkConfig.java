package com.mallowlink.payment.config;

import com.mallowlink.common.repository.InMemoryTokenStore;
import com.mallowlink.common.utils.JsonUtil;
import com.mallowlink.payment.client.MallowlinkProperties;
import feign.Feign;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

import static com.mallowlink.common.repository.TokenRepository.ACCESS_TOKEN_KEY;

@Slf4j
@RequiredArgsConstructor
public class MallowlinkConfig {

    private final JsonUtil jsonUtil;
    private final MallowlinkProperties mallowlinkProperties;
    private final InMemoryTokenStore tokenStore;

    @Bean
    public RequestInterceptor interceptor() {
        return requestTemplate -> {
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Authorization", "Bearer " + tokenStore.getToken(ACCESS_TOKEN_KEY));
            requestTemplate.body(jsonUtil.toJson(requestTemplate.body()));
        };
    }

    @Bean
    public Feign.Builder feignBuilder() {
        return FeignClientConfig.FeignBuilder();
    }
}
