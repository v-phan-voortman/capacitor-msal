package com.voortman.capacitor.msal

import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "Msal")
class MsalPlugin : Plugin() {

    private companion object {
        private const val FALLBACK_AUTHORITY = "https://login.microsoftonline.com/common"
    }

    private lateinit var msalManager: MsalManager
    private lateinit var biometricHelper: BiometricHelper

    override fun load() {
        val authority = getConfig().getString("authority", FALLBACK_AUTHORITY)!!
        val scopesArray: Array<String>? = getConfig().getArray("defaultScopes")
        val defaultScopes: List<String> = scopesArray?.toList()?.ifEmpty { emptyList() } ?: emptyList()

        msalManager = MsalManager(context, authority, defaultScopes)
        biometricHelper = BiometricHelper()
        msalManager.initialize()
    }

    @PluginMethod fun signIn(call: PluginCall)                  = msalManager.signIn(call, activity)
    @PluginMethod fun signOut(call: PluginCall)                 = msalManager.signOut(call)
    @PluginMethod fun getAccount(call: PluginCall)              = msalManager.getAccount(call)
    @PluginMethod fun acquireTokenSilent(call: PluginCall)      = msalManager.acquireTokenSilent(call)
    @PluginMethod fun acquireTokenInteractive(call: PluginCall) = msalManager.acquireTokenInteractive(call, activity)
    @PluginMethod fun unlockWithBiometrics(call: PluginCall)    = biometricHelper.unlock(call, activity, context)
}
