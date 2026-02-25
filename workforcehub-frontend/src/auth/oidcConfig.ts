import type { UserManagerSettings } from 'oidc-client-ts'

const appUrl = import.meta.env.VITE_APP_URL || window.location.origin

/**
 * Builds OIDC settings for a given provider.
 * All flows use PKCE — no client secret is ever stored in the browser.
 */
export function buildOidcSettings(provider: 'google' | 'github' | 'custom'): UserManagerSettings {
  const base: Partial<UserManagerSettings> = {
    redirect_uri:         `${appUrl}/auth/callback`,
    post_logout_redirect_uri: `${appUrl}/login`,
    response_type:        'code',
    scope:                'openid profile email',
    automaticSilentRenew: true,
    silent_redirect_uri:  `${appUrl}/auth/silent`,
    loadUserInfo:         true,
    // PKCE is enabled by default in oidc-client-ts
  }

  switch (provider) {
    case 'google':
      return {
        ...base,
        authority:  import.meta.env.VITE_OIDC_GOOGLE_AUTHORITY || 'https://accounts.google.com',
        client_id:  import.meta.env.VITE_OIDC_GOOGLE_CLIENT_ID || '',
        scope:      'openid profile email',
      } as UserManagerSettings

    case 'github':
      // GitHub doesn't support OIDC natively.
      // In production, federate GitHub through Keycloak or Auth0 which DO support OIDC.
      // This config points to your identity broker (Keycloak realm with GitHub IdP).
      return {
        ...base,
        authority: import.meta.env.VITE_OIDC_GITHUB_AUTHORITY || 'https://accounts.google.com',
        client_id: import.meta.env.VITE_OIDC_GITHUB_CLIENT_ID || '',
        // Keycloak identity hint: `kc_idp_hint=github`
        extraQueryParams: { kc_idp_hint: 'github' },
      } as UserManagerSettings

    case 'custom':
    default:
      // Keycloak / Auth0 / custom IdP
      return {
        ...base,
        authority: import.meta.env.VITE_OIDC_AUTHORITY || 'https://accounts.google.com',
        client_id: import.meta.env.VITE_OIDC_CLIENT_ID || '',
      } as UserManagerSettings
  }
}

/** The active OIDC config — use custom/Keycloak if configured, otherwise Google */
export const activeOidcSettings: UserManagerSettings =
  import.meta.env.VITE_OIDC_AUTHORITY
    ? buildOidcSettings('custom')
    : buildOidcSettings('google')
