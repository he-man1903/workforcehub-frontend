package com.workforce.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared JWT config — bound in each downstream service via @EnableConfigurationProperties.
 * Only needs the secret + issuer (no expiration — services just validate, never issue).
 */
@Getter @Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** Must match the secret in workforce-auth-service */
    private String secret;
    /** Must match the issuer in workforce-auth-service (default: workforcehub) */
    private String issuer = "workforcehub";
}
