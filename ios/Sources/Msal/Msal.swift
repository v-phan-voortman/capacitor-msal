import Foundation
import UIKit
import MSAL

public struct AccountInfo {
    public let username: String
    public let tenantId: String
}

public struct TokenInfo {
    public let accessToken: String
    public let expiresOn: Double
}

public class MsalClient {
    private let application: MSALPublicClientApplication

    public init(clientId: String, authority: String) throws {
        guard let authorityUrl = URL(string: authority) else {
            throw NSError(domain: "MsalClient", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "Invalid authority URL: \(authority)"])
        }
        let msalAuthority = try MSALAADAuthority(url: authorityUrl)
        let config = MSALPublicClientApplicationConfig(
            clientId: clientId,
            redirectUri: nil,
            authority: msalAuthority
        )
        application = try MSALPublicClientApplication(configuration: config)
    }

    public func signIn(scopes: [String], viewController: UIViewController) async throws -> AccountInfo {
        if let account = try application.allAccounts().first {
            let params = MSALSilentTokenParameters(scopes: scopes, account: account)
            if let result = try? await acquireTokenSilentInternal(params: params) {
                return accountInfo(from: result.account)
            }
        }
        let result = try await acquireTokenInteractiveInternal(scopes: scopes, viewController: viewController)
        return accountInfo(from: result.account)
    }

    public func signOut() throws {
        for account in try application.allAccounts() {
            try application.remove(account)
        }
    }

    public func getAccount() throws -> AccountInfo? {
        guard let account = try application.allAccounts().first else { return nil }
        return accountInfo(from: account)
    }

    public func acquireTokenSilent(scopes: [String]) async throws -> TokenInfo {
        guard let account = try application.allAccounts().first else {
            throw interactionRequiredError()
        }
        let params = MSALSilentTokenParameters(scopes: scopes, account: account)
        do {
            let result = try await acquireTokenSilentInternal(params: params)
            return tokenInfo(from: result)
        } catch let error as NSError where isInteractionRequired(error) {
            throw interactionRequiredError()
        }
    }

    public func acquireTokenInteractive(scopes: [String], viewController: UIViewController) async throws -> TokenInfo {
        let result = try await acquireTokenInteractiveInternal(scopes: scopes, viewController: viewController)
        return tokenInfo(from: result)
    }

    // MARK: - Private

    private func acquireTokenSilentInternal(params: MSALSilentTokenParameters) async throws -> MSALResult {
        try await withCheckedThrowingContinuation { continuation in
            application.acquireTokenSilent(with: params) { result, error in
                if let result = result {
                    continuation.resume(returning: result)
                } else {
                    continuation.resume(throwing: error ?? NSError(domain: "MsalClient", code: -1,
                                                                    userInfo: nil))
                }
            }
        }
    }

    private func acquireTokenInteractiveInternal(scopes: [String], viewController: UIViewController) async throws -> MSALResult {
        let webviewParams = MSALWebviewParameters(authPresentationViewController: viewController)
        let params = MSALInteractiveTokenParameters(scopes: scopes, webviewParameters: webviewParams)
        return try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.main.async {
                self.application.acquireToken(with: params) { result, error in
                    if let result = result {
                        continuation.resume(returning: result)
                    } else {
                        continuation.resume(throwing: error ?? NSError(domain: "MsalClient", code: -1,
                                                                        userInfo: nil))
                    }
                }
            }
        }
    }

    private func accountInfo(from account: MSALAccount) -> AccountInfo {
        let tenantId = account.accountClaims?["tid"] as? String ?? ""
        return AccountInfo(username: account.username ?? "", tenantId: tenantId)
    }

    private func tokenInfo(from result: MSALResult) -> TokenInfo {
        let expiresOn = (result.expiresOn?.timeIntervalSince1970 ?? 0) * 1000
        return TokenInfo(accessToken: result.accessToken, expiresOn: expiresOn)
    }

    private func isInteractionRequired(_ error: NSError) -> Bool {
        error.domain == MSALErrorDomain && error.code == MSALError.interactionRequired.rawValue
    }

    private func interactionRequiredError() -> NSError {
        NSError(domain: "MsalClient", code: MSALError.interactionRequired.rawValue,
                userInfo: [NSLocalizedDescriptionKey: "INTERACTION_REQUIRED"])
    }
}
