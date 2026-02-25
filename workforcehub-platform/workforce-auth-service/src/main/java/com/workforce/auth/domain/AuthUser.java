package com.workforce.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted user record, created on first login via Google.
 * Identified by their Google subject (sub) claim — not email,
 * since emails can change on Google accounts.
 */
@Entity
@Table(
    name = "auth_users",
    indexes = {
        @Index(name = "idx_auth_users_google_subject", columnList = "google_subject", unique = true),
        @Index(name = "idx_auth_users_email",          columnList = "email",          unique = true),
        @Index(name = "idx_auth_users_tenant_id",      columnList = "tenant_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Google's immutable subject identifier */
    @Column(name = "google_subject", nullable = false, unique = true, updatable = false)
    private String googleSubject;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "picture_url")
    private String pictureUrl;

    /**
     * Tenant derived from hostedDomain on first login.
     * Falls back to the google_subject if no hosted domain.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Role {
        USER, ADMIN, SUPER_ADMIN
    }
}
