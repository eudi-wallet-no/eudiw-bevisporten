package no.idporten.eudiw.oauth2.server.audit;

import no.idporten.eudiw.oauth2.server.protocol.*;

/**
 * Implements this interface and register with server configuration to get an audit trail.
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
