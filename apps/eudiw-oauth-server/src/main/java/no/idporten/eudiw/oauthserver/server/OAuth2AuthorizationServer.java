package no.idporten.eudiw.oauthserver.server;

import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegrationBase;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.*;

import java.util.Objects;

import static no.idporten.sdk.oidcserver.util.StringUtils.hasText;

public class OAuth2AuthorizationServer extends OpenIDConnectIntegrationBase {


    public OAuth2AuthorizationServer(OpenIDConnectSdkConfiguration sdkConfiguration) {
        super(sdkConfiguration);
    }

    @Override
    public TokenResponse process(TokenRequest tokenRequest) {
        if ("authorization_code".equals(tokenRequest.getGrantType())) {
            return super.process(tokenRequest);
        }
        return processPreAuthorizedTokenRequest(tokenRequest);
    }

    /**
     * Process Pre-authorized token reqyest.  Validate request, lookup authorization, check tx_code and create token response.
     */
    protected TokenResponse processPreAuthorizedTokenRequest(TokenRequest tokenRequest) {
        ClientMetadata clientMetadata = authenticateClient(tokenRequest);
        validate(tokenRequest, clientMetadata);
        getSDKConfiguration().getAuditLogger().auditTokenRequest(tokenRequest);
        Authorization preAuthorization = getSDKConfiguration().getCache().getAuthorization(tokenRequest.getPreAuthorizedCode());
        if (preAuthorization == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant does not exist.", 400);
        }
        if (!preAuthorization.isValidNow()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant has expired.", 400);
        }
        getSDKConfiguration().getCache().removeAuthorization(tokenRequest.getPreAuthorizedCode());
        if (hasText(preAuthorization.getCodeChallenge()) && !validateCodeVerifier(tokenRequest.getTxCode(), preAuthorization.getCodeChallenge())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. Invalid transaction code.", 400);
        }
        if (tokenRequest.hasResourceIndicator()) {
            preAuthorization.setAud(tokenRequest.getResource());
        }
        try {
            TokenResponse tokenResponse = createTokenResponse(preAuthorization);
            getSDKConfiguration().getAuditLogger().auditTokenResponse(tokenResponse);
            return tokenResponse;
        } catch (Exception e) {
            throw new OAuth2Exception("internal_error", "The server failed to process the request", 500, e);
        }
    }

    protected void validatePreAuthorization(Authorization authorization) {
        Objects.requireNonNull(authorization);
        Objects.requireNonNull(authorization.getSub(), "pre-authorization must have a sub");
    }

    /**
     * Process pre-autghorization request.  Create an authorization, store in cache and generate a response
     * with pre.authorization_code.
     */
    public PreAuthorizationResponse process(PreAuthorizationRequest preAuthorizationRequest) {
        String preAuthorizationCode = generateId();
        Authorization preAuthorization = Authorization.builder()
                .sub(preAuthorizationRequest.getSub())
                .aud(preAuthorizationRequest.getAud())
                .scope(String.join(" ", preAuthorizationRequest.getScope()))
                .codeChallenge(preAuthorizationRequest.getTxCodeChallenge())
                .attribute("tx_id", preAuthorizationRequest.getTxId())
                .build();
        preAuthorization.setLifetimeSeconds(preAuthorizationRequest.getAuthorizationLifetimeSeconds() > 0 ? preAuthorizationRequest.getAuthorizationLifetimeSeconds() : getSDKConfiguration().getAuthorizationLifetimeSeconds());
        validatePreAuthorization(preAuthorization);
        getSDKConfiguration().getCache().putAuthorization(preAuthorizationCode, preAuthorization);
        getSDKConfiguration().getAuditLogger().auditAuthorization(preAuthorization);
        PreAuthorizationResponse preAuthorizationResponse = PreAuthorizationResponse.builder()
                .preAuthorizedCode(preAuthorizationCode)
                .expiresInSeconds(preAuthorization.expiresInSeconds())
                .build();
        return preAuthorizationResponse;
    }

}
