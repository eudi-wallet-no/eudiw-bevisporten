package no.idporten.sdk.oidcserver.audit;

import no.idporten.sdk.oidcserver.protocol.*;

/**
 * Implements this interface and register with SDK configuration to get an audit trail.
 */
public interface OpenIDConnectAuditLogger {

    void auditClientAuthentication(ClientAuthentication clientAuthentication);
    void auditPushedAuthorizationRequest(PushedAuthorizationRequest pushedAuthorizationRequest);
    void auditPushedAuthorizationResponse(PushedAuthorizationResponse pushedAuthorizationResponse);
    void auditAuthorizationRequest(AuthorizationRequest authorizationRequest);
    void auditAuthorizationResponse(AuthorizationResponse authorizationResponse);
    void auditAuthorization(Authorization authorization);
    void auditTokenRequest(TokenRequest tokenRequest);
    void auditTokenResponse(TokenResponse tokenResponse);
    default void auditUserInfoRequest(UserInfoRequest userInfoRequest) {}
    default void auditUserInfoResponse(UserInfoResponse userInfoResponse) {}

}
