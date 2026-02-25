package com.workforce.auth.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * Extracted, verified claims from a Google ID token.
 * Produced by {@link com.workforce.auth.security.GoogleTokenVerifierService} after
 * signature/expiry/audience validation. All fields are safe to trust at this point.
 */
@Getter
@Builder
public class GoogleIdTokenPayload {

    /** Google's immutable, stable user identifier (the `sub` claim). Never changes even if email changes. */
    private final String subject;

    /** Verified email address. */
    private final String email;

    /** True if Google has verified this email address belongs to the user. */
    private final boolean emailVerified;

    /** Display name from Google profile (may be null). */
    private final String name;

    /** Profile picture URL from Google (may be null). */
    private final String pictureUrl;

    /**
     * Google Workspace hosted domain (the `hd` claim).
     * Non-null only for Workspace / GSuite accounts (e.g. "acme.com").
     * Null for personal @gmail.com accounts.
     */
    private final String hostedDomain;
}
