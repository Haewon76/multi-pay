package com.mallowlink.payment.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Base properties class for external service configurations.
 * Contains common properties like url, username, and password.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalServiceProperties {
    private String gatewayUrl;
    private String url;
    private String username;
    private String password;
}