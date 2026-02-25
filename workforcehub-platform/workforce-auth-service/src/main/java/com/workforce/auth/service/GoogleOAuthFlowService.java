package com.workforce.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforce.auth.config.AppProperties;
import com.workforce.auth.config.GoogleProperties;
import com.workforce.auth.dto.response.AuthResponse;
import com.workforce.auth.exception.InvalidGoogleTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Server-side Google OAuth2 authorization code flow.
 * Initiates login redirect and exchanges the code for tokens (with
 * client_secret) on the backend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthFlowService {

    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPES = "openid email profile";

    private final GoogleProperties googleProperties;
    private final AppProperties appProperties;
    private final AuthService authService;
    private final OAuth2StateStore stateStore;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Builds the Google authorization URL and returns a state token to store.
     * Redirect the user to the returned URL; store the state and pass it to
     * handleCallback.
     */
    public String buildLoginRedirectUrl() {
        String clientId = googleProperties.getClientId();
        String gatewayUrl = appProperties.getGatewayPublicUrl();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_CLIENT_ID is not set. Set it in your environment or application.yml.");
        }
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            throw new IllegalStateException("app.gateway-public-url (GATEWAY_PUBLIC_URL) is not set.");
        }

        String state = UUID.randomUUID().toString();
        stateStore.save(state);

        String redirectUri = gatewayUrl + "/auth/oauth2/callback";
        String scope = URLEncoder.encode(SCOPES, StandardCharsets.UTF_8);
        String stateEnc = URLEncoder.encode(state, StandardCharsets.UTF_8);
        String redirectEnc = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        return AUTHORIZE_URL
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + redirectEnc
                + "&response_type=code"
                + "&scope=" + scope
                + "&state=" + stateEnc
                + "&access_type=offline"
                + "&prompt=consent";
    }

    /**
     * Exchanges the authorization code for tokens, verifies the ID token, and
     * issues our JWT.
     *
     * @param code  from Google callback
     * @param state from Google callback (must match stored state)
     * @return AuthResponse with access and refresh tokens
     */
    public AuthResponse handleCallback(String code, String state) {
        if (code == null || code.isBlank()) {
            throw new InvalidGoogleTokenException("Missing authorization code");
        }
        if (state == null || !stateStore.consume(state)) {
            log.warn("Invalid or reused OAuth state: {}", state);
            throw new InvalidGoogleTokenException("Invalid or expired state; please try again");
        }

        String clientSecret = googleProperties.getClientSecret();
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_CLIENT_SECRET is not set. Set it in your environment or application.yml.");
        }

        String gatewayUrl = appProperties.getGatewayPublicUrl();
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            throw new IllegalStateException("app.gateway-public-url (GATEWAY_PUBLIC_URL) is not set.");
        }

        String redirectUri = gatewayUrl + "/auth/oauth2/callback";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleProperties.getClientId());
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    TOKEN_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
        } catch (Exception e) {
            log.warn("Google token request failed: {}", e.getMessage());
            throw new InvalidGoogleTokenException("Google token exchange failed: " + e.getMessage(), e);
        }

        String responseBody = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful()) {
            String msg = "Google token exchange failed: " + response.getStatusCode();
            if (responseBody != null && !responseBody.isBlank()) {
                try {
                    JsonNode err = objectMapper.readTree(responseBody);
                    if (err.has("error_description"))
                        msg = msg + " — " + err.get("error_description").asText();
                    else if (err.has("error"))
                        msg = msg + " — " + err.get("error").asText();
                } catch (JsonProcessingException ignored) {
                }
            }
            log.warn(msg);
            throw new InvalidGoogleTokenException(msg);
        }
        if (responseBody == null || responseBody.isBlank()) {
            throw new InvalidGoogleTokenException("Google returned an empty response");
        }

        JsonNode json;
        try {
            json = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new InvalidGoogleTokenException("Invalid Google token response: " + e.getMessage(), e);
        }
        String idToken = Optional.ofNullable(json.get("id_token"))
                .map(JsonNode::asText)
                .orElseThrow(() -> new InvalidGoogleTokenException("Google did not return an id_token"));

        return authService.loginWithGoogle(idToken);
    }

    public URI getFrontendCallbackUri(String accessToken, String refreshToken) {
        String base = appProperties.getFrontendUrl().replaceAll("/$", "");
        String url = base + "/auth/backend-callback?token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refresh=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        return URI.create(url);
    }
}

