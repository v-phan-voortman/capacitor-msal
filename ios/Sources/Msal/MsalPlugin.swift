import Foundation
import Capacitor
import LocalAuthentication
import MSAL

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

    private var msalClient: MsalClient?

    private func client() throws -> MsalClient {
        if let existing = msalClient { return existing }
        guard let clientId = getConfig().getString("clientId"), !clientId.isEmpty else {
            throw NSError(domain: "MsalPlugin", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "clientId is required in capacitor.config under plugins.Msal"])
        }
        let authority = getConfig().getString("authority") ?? "https://login.microsoftonline.com/common"
        let instance = try MsalClient(clientId: clientId, authority: authority)
        msalClient = instance
        return instance
    }

    // Extracts the human-readable MSAL error description when available.
    private func msalMessage(_ error: Error) -> String {
        let ns = error as NSError
        if let desc = ns.userInfo[MSALErrorDescriptionKey] as? String { return desc }
        if let oauth = ns.userInfo[MSALOAuthErrorKey] as? String {
            let sub = ns.userInfo[MSALOAuthSubErrorKey] as? String ?? ""
            return "OAuth error: \(oauth)\(sub.isEmpty ? "" : " (\(sub))")"
        }
        if let underlying = ns.userInfo[NSUnderlyingErrorKey] as? NSError {
            return underlying.localizedDescription
        }
        return ns.localizedDescription
    }

    @objc func signIn(_ call: CAPPluginCall) {
        let scopes = call.getArray("scopes", String.self) ?? []
        Task { @MainActor in
            guard let vc = self.bridge?.viewController else {
                call.reject("No view controller available")
                return
            }
            do {
                let info = try await self.client().signIn(scopes: scopes, viewController: vc)
                call.resolve(["username": info.username, "tenantId": info.tenantId])
            } catch {
                call.reject(self.msalMessage(error))
            }
        }
    }

    @objc func signOut(_ call: CAPPluginCall) {
        do {
            try client().signOut()
            call.resolve()
        } catch {
            call.reject(msalMessage(error))
        }
    }

    @objc func getAccount(_ call: CAPPluginCall) {
        do {
            if let info = try client().getAccount() {
                call.resolve(["account": ["username": info.username, "tenantId": info.tenantId]])
            } else {
                call.resolve(["account": NSNull()])
            }
        } catch {
            call.reject(msalMessage(error))
        }
    }

    @objc func acquireTokenSilent(_ call: CAPPluginCall) {
        let scopes = call.getArray("scopes", String.self) ?? []
        Task {
            do {
                let info = try await self.client().acquireTokenSilent(scopes: scopes)
                call.resolve(["accessToken": info.accessToken, "expiresOn": info.expiresOn])
            } catch {
                call.reject(self.msalMessage(error))
            }
        }
    }

    @objc func acquireTokenInteractive(_ call: CAPPluginCall) {
        let scopes = call.getArray("scopes", String.self) ?? []
        Task { @MainActor in
            guard let vc = self.bridge?.viewController else {
                call.reject("No view controller available")
                return
            }
            do {
                let info = try await self.client().acquireTokenInteractive(scopes: scopes, viewController: vc)
                call.resolve(["accessToken": info.accessToken, "expiresOn": info.expiresOn])
            } catch {
                call.reject(self.msalMessage(error))
            }
        }
    }

    @objc func unlockWithBiometrics(_ call: CAPPluginCall) {
        let reason = call.getString("title") ?? "Authenticate"
        let cancelTitle = call.getString("cancelButtonText") ?? "Cancel"
        Task {
            let context = LAContext()
            context.localizedCancelTitle = cancelTitle
            var policyError: NSError?
            guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &policyError) else {
                call.reject(policyError?.localizedDescription ?? "Biometrics not available on this device")
                return
            }
            do {
                try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                    context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, error in
                        if success {
                            continuation.resume()
                        } else {
                            continuation.resume(throwing: error ?? NSError(
                                domain: LAErrorDomain,
                                code: LAError.authenticationFailed.rawValue,
                                userInfo: nil))
                        }
                    }
                }
                call.resolve()
            } catch {
                call.reject(error.localizedDescription)
            }
        }
    }
}
