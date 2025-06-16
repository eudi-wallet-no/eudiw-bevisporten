package no.idporten.eudiw.oauthserver.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditIdentifier;
import no.idporten.logging.audit.AuditLogger;
import no.idporten.sdk.oidcserver.audit.OpenIDConnectAuditLogger;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService implements OpenIDConnectAuditLogger {

    private final AuditLogger auditLogger;

    @Getter
    @AllArgsConstructor
    enum AuditIdPattern {

        OAUTH_CLIENT_AUTHENTICATION("%s-AUTHENTICATE-CLIENT"),
        OAUTH_PAR_REQUEST("%s-RECEIVE-PUSHED-AUTHORIZATION-REQUEST"),
        OAUTH_PAR_RESPONSE("%s-SEND-PUSHED-AUTHORIZATION-RESPONSE"),
        OAUTH_AUTHORIZATION_REQUEST("%s-RECEIVE-AUTHORIZATION-REQUEST"),
        OAUTH_AUTHORIZE_USER("%s-AUTHORIZE-USER"),
        OAUTH_AUTHORIZATION_RESPONSE("%s-SEND-AUTHORIZATION-RESPONSE"),
        OAUTH_TOKEN_REQUEST("%s-RECEIVE-TOKEN-REQUEST"),
        OAUTH_TOKEN_RESPONSE("%s-SEND-TOKEN-RESPONSE");

        private final String pattern;

        AuditIdentifier auditIdentifier() {
            return () -> getPattern().formatted("EUDIW-OAUTH-SERVER");
        }
    }

    @Override
    public void auditClientAuthentication(ClientAuthentication clientAuthentication) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OAUTH_CLIENT_AUTHENTICATION.auditIdentifier())
                .logNullAttributes(false)
                .attributes(clientAuthentication.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditPushedAuthorizationRequest(PushedAuthorizationRequest pushedAuthorizationRequest) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OAUTH_PAR_REQUEST.auditIdentifier())
                .logNullAttributes(false)
                .attribute("pushed_authorization_request", pushedAuthorizationRequest.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditPushedAuthorizationResponse(PushedAuthorizationResponse pushedAuthorizationResponse) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OAUTH_PAR_RESPONSE.auditIdentifier())
                .logNullAttributes(false)
                .attribute("pushed_authorization_response", pushedAuthorizationResponse.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OAUTH_AUTHORIZATION_REQUEST.auditIdentifier())
                .logNullAttributes(false)
                .attribute("authorization_request", authorizationRequest.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditAuthorizationResponse(AuthorizationResponse authorizationResponse) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OAUTH_AUTHORIZATION_RESPONSE.auditIdentifier())
                .logNullAttributes(false)
                .attribute("authorization_response", authorizationResponse.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditAuthorization(Authorization authorization) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OAUTH_AUTHORIZE_USER.auditIdentifier())
                .logNullAttributes(false)
                .attribute("authorization", authorization.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditTokenRequest(TokenRequest tokenRequest) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OAUTH_TOKEN_REQUEST.auditIdentifier())
                .logNullAttributes(false)
                .attribute("token_request", tokenRequest.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditTokenResponse(TokenResponse tokenResponse) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OAUTH_TOKEN_RESPONSE.auditIdentifier())
                .logNullAttributes(false)
                .attribute("token_response", tokenResponse.getAuditData().getAttributes())
                .build());
    }

}
