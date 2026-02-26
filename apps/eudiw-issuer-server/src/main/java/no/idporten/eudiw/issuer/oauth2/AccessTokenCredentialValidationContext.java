package no.idporten.eudiw.issuer.oauth2;


/**
 * Context for access_token validation specific to a credential configuration.
 */
public record AccessTokenCredentialValidationContext(
        String authorizationServer,
        String preAuthorizationServer,
        String scope
) {

    /**
     * Context for authorization server.  Use in OpenID4VCI endpoints.
     */
    public static AccessTokenCredentialValidationContext forAuthorization(String authorizationServer, String scope) {
        return new AccessTokenCredentialValidationContext(authorizationServer, null, scope);
    }

    /**
     * Context for pre-authorization server.  Use in issuer extended API.
     */
    public static AccessTokenCredentialValidationContext forPreAuthorization(String preAuthorizationServer, String scope) {
        return new AccessTokenCredentialValidationContext(null, preAuthorizationServer, scope);
    }

}
