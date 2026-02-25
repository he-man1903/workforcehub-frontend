package com.workforce.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {
    /** OAuth2 client ID registered in Google Cloud Console */
    private String clientId;
    /** OAuth2 client secret — required for server-side code exchange */
    private String clientSecret;
    /** Optional: restrict login to specific GSuite/Workspace domains */
    private List<String> allowedDomains = List.of();
}
