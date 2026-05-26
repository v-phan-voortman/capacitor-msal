import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(MsalPlugin)
public class MsalPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "MsalPlugin"
    public let jsName = "Msal"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "signIn", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "signOut", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getAccount", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "acquireTokenSilent", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "acquireTokenInteractive", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "unlockWithBiometrics", returnType: CAPPluginReturnPromise),
    ]

    @objc func signIn(_ call: CAPPluginCall) {
        call.unimplemented("iOS MSAL implementation pending.")
    }

    @objc func signOut(_ call: CAPPluginCall) {
        call.unimplemented("iOS MSAL implementation pending.")
    }

    @objc func getAccount(_ call: CAPPluginCall) {
        call.unimplemented("iOS MSAL implementation pending.")
    }

    @objc func acquireTokenSilent(_ call: CAPPluginCall) {
        call.unimplemented("iOS MSAL implementation pending.")
    }

    @objc func acquireTokenInteractive(_ call: CAPPluginCall) {
        call.unimplemented("iOS MSAL implementation pending.")
    }

    @objc func unlockWithBiometrics(_ call: CAPPluginCall) {
        call.unimplemented("iOS MSAL implementation pending.")
    }
}

