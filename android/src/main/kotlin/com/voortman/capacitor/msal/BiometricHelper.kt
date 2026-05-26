package com.voortman.capacitor.msal

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.getcapacitor.PluginCall

class BiometricHelper {

    fun unlock(call: PluginCall, activity: AppCompatActivity, context: Context) {
        val title = call.getString("title", "Authenticate") ?: "Authenticate"
        val subtitle = call.getString("subtitle", "") ?: ""
        val cancelText = call.getString("cancelButtonText", "Cancel") ?: "Cancel"

        val canAuth = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            call.reject("BIOMETRIC_UNAVAILABLE", "No biometric hardware enrolled or available.")
            return
        }

        activity.runOnUiThread {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    call.resolve()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    call.reject(errString.toString(), errorCode.toString())
                }
                override fun onAuthenticationFailed() {
                    // Individual failure — keep prompt open, do nothing
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancelText)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                .build()

            try {
                prompt.authenticate(promptInfo)
            } catch (e: Exception) {
                call.reject(e.message ?: "BiometricPrompt error")
            }
        }
    }
}
