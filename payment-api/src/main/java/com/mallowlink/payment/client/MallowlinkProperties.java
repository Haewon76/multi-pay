package com.mallowlink.payment.client;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Tiger API service.
 * Binds to configuration properties with prefix "external.service.tiger".
 */
@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "external.service.tiger")
public class MallowlinkProperties extends ExternalServiceProperties {

    public MallowlinkProperties(String gatewayUrl, String url, String username, String password) {
        super(gatewayUrl, url, username, password);
    }
}
