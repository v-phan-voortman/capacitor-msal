import { WebPlugin } from '@capacitor/core';

import type { Account, BiometricOptions, MsalPlugin, TokenResult } from './definitions';

export class MsalWeb extends WebPlugin implements MsalPlugin {
  async signIn(_options: { scopes: string[] }): Promise<Account> {
    throw this.unimplemented('Msal is not available on web.');
  }

  async signOut(): Promise<void> {
    throw this.unimplemented('Msal is not available on web.');
  }

  async getAccount(): Promise<Account | null> {
    throw this.unimplemented('Msal is not available on web.');
  }

  async acquireTokenSilent(_options: { scopes: string[] }): Promise<TokenResult> {
    throw this.unimplemented('Msal is not available on web.');
  }

  async acquireTokenInteractive(_options: { scopes: string[] }): Promise<TokenResult> {
    throw this.unimplemented('Msal is not available on web.');
  }

  async unlockWithBiometrics(_options?: BiometricOptions): Promise<void> {
    throw this.unimplemented('Msal is not available on web.');
  }
}
