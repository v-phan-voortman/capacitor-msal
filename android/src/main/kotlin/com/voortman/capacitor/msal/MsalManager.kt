package com.voortman.capacitor.msal

import android.app.Activity
import android.content.Context
import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException

class MsalManager(
    private val context: Context,
    private val authority: String,
    private val defaultScopes: List<String>
) {
    companion object {
        private const val TAG = "MsalManager"
    }

    private var pca: ISingleAccountPublicClientApplication? = null
    private var initError: String? = null

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    fun initialize() {
        val resId = context.resources.getIdentifier("auth_config", "raw", context.packageName)
        if (resId == 0) {
            initError = "auth_config.json not found in app raw resources"
            Log.e(TAG, initError!!)
            return
        }

        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            resId,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    pca = application
                    Log.i(TAG, "MSAL initialized")
                }

                override fun onError(exception: MsalException) {
                    initError = msalError(exception)
                    Log.e(TAG, "MSAL init failed: $initError", exception)
                }
            }
        )
    }

    // ── signIn ─────────────────────────────────────────────────────────────────

    fun signIn(call: PluginCall, activity: Activity) {
        val pca = requirePca(call) ?: return
        val scopes = scopesFrom(call)

        pca.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(account: IAccount?) {
                if (account != null) {
                    val params = AcquireTokenSilentParameters.Builder()
                        .withScopes(scopes)
                        .forAccount(account)
                        .fromAuthority(authority)
                        .withCallback(object : SilentAuthenticationCallback {
                            override fun onSuccess(result: IAuthenticationResult) {
                                call.resolve(accountToJS(result.account))
                            }
                            override fun onError(exception: MsalException) {
                                pca.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                                    override fun onSignOut() { startInteractiveSignIn(call, pca, scopes, activity) }
                                    override fun onError(e: MsalException) { startInteractiveSignIn(call, pca, scopes, activity) }
                                })
                            }
                        })
                        .build()
                    pca.acquireTokenSilentAsync(params)
                } else {
                    startInteractiveSignIn(call, pca, scopes, activity)
                }
            }

            // Single-account mode: account changes are handled by the calling flow
            override fun onAccountChanged(prior: IAccount?, current: IAccount?) {}

            override fun onError(exception: MsalException) {
                call.reject(msalError(exception))
            }
        })
    }

    private fun startInteractiveSignIn(
        call: PluginCall,
        pca: ISingleAccountPublicClientApplication,
        scopes: List<String>,
        activity: Activity
    ) {
        val params = SignInParameters.builder()
            .withScopes(scopes)
            .withActivity(activity)
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(result: IAuthenticationResult) {
                    call.resolve(accountToJS(result.account))
                }
                override fun onError(exception: MsalException) {
                    call.reject(msalError(exception))
                }
                override fun onCancel() {
                    call.reject("USER_CANCELLED")
                }
            })
            .build()
        pca.signIn(params)
    }

    // ── signOut ────────────────────────────────────────────────────────────────

    fun signOut(call: PluginCall) {
        val pca = requirePca(call) ?: return
        pca.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() { call.resolve() }
            override fun onError(exception: MsalException) { call.reject(msalError(exception)) }
        })
    }

    // ── getAccount ─────────────────────────────────────────────────────────────

    fun getAccount(call: PluginCall) {
        val pca = requirePca(call) ?: return
        pca.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(account: IAccount?) {
                call.resolve(if (account != null) accountToJS(account) else JSObject())
            }
            // Single-account mode: account changes are handled by the calling flow
            override fun onAccountChanged(prior: IAccount?, current: IAccount?) {}
            override fun onError(exception: MsalException) { call.reject(msalError(exception)) }
        })
    }

    // ── acquireTokenSilent ─────────────────────────────────────────────────────

    fun acquireTokenSilent(call: PluginCall) {
        val pca = requirePca(call) ?: return
        val scopes = scopesFrom(call)

        pca.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(account: IAccount?) {
                if (account == null) {
                    call.reject("NO_ACCOUNT")
                    return
                }
                val params = AcquireTokenSilentParameters.Builder()
                    .withScopes(scopes)
                    .forAccount(account)
                    .fromAuthority(authority)
                    .withCallback(object : SilentAuthenticationCallback {
                        override fun onSuccess(result: IAuthenticationResult) {
                            call.resolve(tokenToJS(result))
                        }
                        override fun onError(exception: MsalException) {
                            if (exception is MsalUiRequiredException) {
                                call.reject("INTERACTION_REQUIRED")
                            } else {
                                call.reject(msalError(exception))
                            }
                        }
                    })
                    .build()
                pca.acquireTokenSilentAsync(params)
            }

            // Single-account mode: account changes are handled by the calling flow
            override fun onAccountChanged(prior: IAccount?, current: IAccount?) {}
            override fun onError(exception: MsalException) { call.reject(msalError(exception)) }
        })
    }

    // ── acquireTokenInteractive ────────────────────────────────────────────────

    fun acquireTokenInteractive(call: PluginCall, activity: Activity) {
        val pca = requirePca(call) ?: return
        val scopes = scopesFrom(call)
        // Sign out any cached account so MSAL allows a fresh interactive sign-in
        pca.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() { startInteractiveForToken(call, pca, scopes, activity) }
            override fun onError(e: MsalException) { startInteractiveForToken(call, pca, scopes, activity) }
        })
    }

    private fun startInteractiveForToken(
        call: PluginCall,
        pca: ISingleAccountPublicClientApplication,
        scopes: List<String>,
        activity: Activity
    ) {
        val params = SignInParameters.builder()
            .withScopes(scopes)
            .withActivity(activity)
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(result: IAuthenticationResult) {
                    call.resolve(tokenToJS(result))
                }
                override fun onError(exception: MsalException) {
                    call.reject(msalError(exception))
                }
                override fun onCancel() {
                    call.reject("USER_CANCELLED")
                }
            })
            .build()
        pca.signIn(params)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun requirePca(call: PluginCall): ISingleAccountPublicClientApplication? {
        val p = pca
        if (p == null) {
            call.reject(initError?.let { "MSAL init failed: $it" } ?: "MSAL not initialized")
            return null
        }
        return p
    }

    private fun scopesFrom(call: PluginCall): List<String> {
        val arr = call.getArray("scopes") ?: return defaultScopes
        return try {
            List(arr.length()) { i -> arr.getString(i) }.ifEmpty { defaultScopes }
        } catch (_: Exception) {
            defaultScopes
        }
    }

    private fun accountToJS(account: IAccount): JSObject {
        val obj = JSObject()
        obj.put("username", account.username)
        obj.put("tenantId", account.tenantId)
        return obj
    }

    private fun tokenToJS(result: IAuthenticationResult): JSObject {
        val obj = JSObject()
        obj.put("accessToken", result.accessToken)
        obj.put("expiresOn", result.expiresOn.time)
        return obj
    }

    private fun msalError(exception: MsalException): String {
        val prefix = when (exception) {
            is MsalClientException -> "CLIENT_ERROR: ${exception.errorCode}"
            is MsalServiceException -> "SERVICE_ERROR: ${exception.errorCode}"
            else -> exception.errorCode
        }
        val detail = exception.message
        return if (!detail.isNullOrEmpty()) "$prefix — $detail" else prefix
    }
}
