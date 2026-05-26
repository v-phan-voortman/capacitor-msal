/**
 * Plugin configuration set in capacitor.config.ts under plugins.Msal.
 * Consumed by the Android native layer at startup; iOS is not yet implemented.
 *
 * @example
 * ```typescript
 * plugins: {
 *   Msal: {
 *     authority: 'https://login.microsoftonline.com/YOUR_TENANT_ID',
 *     defaultScopes: ['api://YOUR_CLIENT_ID/.default'],
 *   } satisfies MsalPluginConfig,
 * },
 * ```
 */
export interface MsalPluginConfig {
  /**
   * MSAL authority URL. Defaults to https://login.microsoftonline.com/common if not set.
   * Example: "https://login.microsoftonline.com/YOUR_TENANT_ID"
   */
  authority?: string;
  /**
   * Default OAuth2 scopes used when no scopes are passed to a method call.
   * Example: ["api://YOUR_CLIENT_ID/.default"]
   */
  defaultScopes?: string[];
}

export interface Account {
  username: string;
  tenantId: string;
}

export interface TokenResult {
  accessToken: string;
  /** Unix timestamp in milliseconds */
  expiresOn: number;
}

export interface BiometricOptions {
  title?: string;
  subtitle?: string;
  cancelButtonText?: string;
}

export interface MsalPlugin {
  /**
   * Sign in interactively via Chrome Custom Tabs (Android) / ASWebAuthenticationSession (iOS).
   * Silent token acquisition is attempted first if a cached account exists.
   */
  signIn(options: { scopes: string[] }): Promise<Account>;

  signOut(): Promise<void>;

  /** Returns null when no account is cached. */
  getAccount(): Promise<Account | null>;

  /**
   * Acquire an access token silently from the OS token cache.
   * Rejects with "INTERACTION_REQUIRED" when the refresh token has expired.
   */
  acquireTokenSilent(options: { scopes: string[] }): Promise<TokenResult>;

  /**
   * Open the browser for a fresh interactive authentication.
   * Use as fallback when acquireTokenSilent rejects with INTERACTION_REQUIRED.
   */
  acquireTokenInteractive(options: { scopes: string[] }): Promise<TokenResult>;

  /**
   * Show a biometric prompt (fingerprint / Face ID).
   * Resolves on success, rejects on failure or cancellation.
   */
  unlockWithBiometrics(options?: BiometricOptions): Promise<void>;
}
