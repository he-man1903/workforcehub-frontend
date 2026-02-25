package com.workforce.auth.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.workforce.auth.config.GoogleProperties;
import com.workforce.auth.domain.GoogleIdTokenPayload;
import com.workforce.auth.exception.InvalidGoogleTokenException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Verifies a Google ID token by:
 * 1. Checking the token signature against Google's public keys (fetched from Google's JWKS endpoint)
 * 2. Validating audience matches our configured client ID
 * 3. Validating issuer is accounts.google.com
 * 4. Checking token expiry
 * 5. Optionally enforcing hosted domain restriction (hd claim)
 *
 * This is the ONLY correct way to trust a Google ID token on the backend.
 * Never decode JWT manually for Google tokens — always use the verifier.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifierService {

    private final GoogleProperties googleProperties;
    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void init() {
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(googleProperties.getClientId()))
                // Google accepts tokens issued by either of these
                .setIssuers(List.of("accounts.google.com", "https://accounts.google.com"))
                .build();

        log.info("GoogleIdTokenVerifier initialised for clientId={}",
                googleProperties.getClientId().substring(0, Math.min(12, googleProperties.getClientId().length())) + "…");
    }

    /**
     * Verifies the raw Google ID token string and returns extracted claims.
     * Throws {@link InvalidGoogleTokenException} on any verification failure.
     */
    public GoogleIdTokenPayload verify(String rawIdToken) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(rawIdToken);
        } catch (Exception e) {
            log.warn("Google ID token verification threw exception: {}", e.getMessage());
            throw new InvalidGoogleTokenException("Google token verification failed: " + e.getMessage(), e);
        }

        if (idToken == null) {
            throw new InvalidGoogleTokenException("Google ID token is invalid or could not be verified");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // Enforce email verification
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidGoogleTokenException("Google account email is not verified");
        }

        String email = payload.getEmail();
        String hostedDomain = (String) payload.get("hd");  // null for @gmail.com accounts

        // Enforce hosted domain restriction if configured
        List<String> allowedDomains = googleProperties.getAllowedDomains();
        if (!allowedDomains.isEmpty()) {
            String emailDomain = email.substring(email.indexOf('@') + 1);
            boolean domainAllowed = allowedDomains.contains(emailDomain) ||
                    (hostedDomain != null && allowedDomains.contains(hostedDomain));
            if (!domainAllowed) {
                log.warn("Login rejected — email domain '{}' not in allowed domains: {}", emailDomain, allowedDomains);
                throw new InvalidGoogleTokenException(
                        "Your account domain is not authorised to access this application");
            }
        }

        return GoogleIdTokenPayload.builder()
                .subject(payload.getSubject())
                .email(email)
                .emailVerified(Boolean.TRUE.equals(payload.getEmailVerified()))
                .name((String) payload.get("name"))
                .pictureUrl((String) payload.get("picture"))
                .hostedDomain(hostedDomain)
                .build();
    }
}
