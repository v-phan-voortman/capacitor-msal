# @vdphan/capacitor-msal

Capacitor plugin for MSAL

## Install

To use npm

```bash
npm install @vdphan/capacitor-msal
````

To use yarn

```bash
yarn add @vdphan/capacitor-msal
```

Sync native files

```bash
npx cap sync
```

## API

<docgen-index>

* [`signIn(...)`](#signin)
* [`signOut()`](#signout)
* [`getAccount()`](#getaccount)
* [`acquireTokenSilent(...)`](#acquiretokensilent)
* [`acquireTokenInteractive(...)`](#acquiretokeninteractive)
* [`unlockWithBiometrics(...)`](#unlockwithbiometrics)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### signIn(...)

```typescript
signIn(options: { scopes: string[]; }) => Promise<Account>
```

Sign in interactively via Chrome Custom Tabs (Android) / ASWebAuthenticationSession (iOS).
Silent token acquisition is attempted first if a cached account exists.

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ scopes: string[]; }</code> |

**Returns:** <code>Promise&lt;<a href="#account">Account</a>&gt;</code>

--------------------


### signOut()

```typescript
signOut() => Promise<void>
```

--------------------


### getAccount()

```typescript
getAccount() => Promise<{ account: Account | null; }>
```

Returns null account when no account is cached.

**Returns:** <code>Promise&lt;{ account: <a href="#account">Account</a> | null; }&gt;</code>

--------------------


### acquireTokenSilent(...)

```typescript
acquireTokenSilent(options: { scopes: string[]; }) => Promise<TokenResult>
```

Acquire an access token silently from the OS token cache.
Rejects with "INTERACTION_REQUIRED" when the refresh token has expired.

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ scopes: string[]; }</code> |

**Returns:** <code>Promise&lt;<a href="#tokenresult">TokenResult</a>&gt;</code>

--------------------


### acquireTokenInteractive(...)

```typescript
acquireTokenInteractive(options: { scopes: string[]; }) => Promise<TokenResult>
```

Open the browser for a fresh interactive authentication.
Use as fallback when acquireTokenSilent rejects with INTERACTION_REQUIRED.

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ scopes: string[]; }</code> |

**Returns:** <code>Promise&lt;<a href="#tokenresult">TokenResult</a>&gt;</code>

--------------------


### unlockWithBiometrics(...)

```typescript
unlockWithBiometrics(options?: BiometricOptions | undefined) => Promise<void>
```

Show a biometric prompt (fingerprint / Face ID).
Resolves on success, rejects on failure or cancellation.

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#biometricoptions">BiometricOptions</a></code> |

--------------------


### Interfaces


#### Account

| Prop           | Type                |
| -------------- | ------------------- |
| **`username`** | <code>string</code> |
| **`tenantId`** | <code>string</code> |


#### TokenResult

| Prop              | Type                | Description                    |
| ----------------- | ------------------- | ------------------------------ |
| **`accessToken`** | <code>string</code> |                                |
| **`expiresOn`**   | <code>number</code> | Unix timestamp in milliseconds |


#### BiometricOptions

| Prop                   | Type                |
| ---------------------- | ------------------- |
| **`title`**            | <code>string</code> |
| **`subtitle`**         | <code>string</code> |
| **`cancelButtonText`** | <code>string</code> |

</docgen-api>
