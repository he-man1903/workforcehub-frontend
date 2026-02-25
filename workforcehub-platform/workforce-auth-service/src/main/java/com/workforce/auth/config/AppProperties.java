package com.workforce.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /** Frontend base URL — where to redirect after successful login */
    private String frontendUrl = "http://localhost:5173";
    /**
     * Public gateway URL — used as redirect_uri base for Google OAuth (e.g.
     * http://localhost:8080)
     */
    private String gatewayPublicUrl = "http://localhost:8080";
}

