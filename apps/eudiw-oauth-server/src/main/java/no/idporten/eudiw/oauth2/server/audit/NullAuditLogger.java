package no.idporten.eudiw.oauth2.server.audit;

import no.idporten.eudiw.oauth2.server.protocol.*;

public class NullAuditLogger implements OpenIDConnectAuditLogger {

    @Override
    public void auditChallengeResponse(ChallengeResponse challengeResponse) {
    }

    @Override
    public void auditClientAuthentication(ClientAuthentication clientAuthentication) {
    }

    @Override
    public void auditPushedAuthorizationRequest(PushedAuthorizationRequest pushedAuthorizationRequest) {
    }

    @Override
    public void auditPushedAuthorizationResponse(PushedAuthorizationResponse pushedAuthorizationResponse) {
    }

    @Override
    public void auditAuthorizationRequest(AuthorizationRequest authorizationRequest) {
    }

    @Override
    public void auditAuthorizationResponse(AuthorizationResponse authorizationResponse) {
    }

    @Override
    public void auditAuthorization(Authorization authorization) {
    }

    @Override
    public void auditTokenRequest(TokenRequest tokenRequest) {
    }

    @Override
    public void auditTokenResponse(TokenResponse tokenResponse) {
    }

    @Override
    public void auditUserInfoRequest(UserInfoRequest userInfoRequest) {
    }

    @Override
    public void auditUserInfoResponse(UserInfoResponse userInfoResponse) {
    }

}
