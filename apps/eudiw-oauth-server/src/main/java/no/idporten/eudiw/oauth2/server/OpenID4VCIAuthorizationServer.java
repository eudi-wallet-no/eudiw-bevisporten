package no.idporten.eudiw.oauth2.server;

import no.idporten.eudiw.oauth2.server.protocol.*;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.util.MultiValuedMapUtils;
import no.idporten.eudiw.oauth2.server.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static no.idporten.eudiw.oauth2.server.util.StringUtils.hasText;

public class OpenID4VCIAuthorizationServer extends OAuth2AuthorizationServerBase {

    public final static String X_API_KEY_HEADER = "X-API-KEY";

    public OpenID4VCIAuthorizationServer(OAuth2ServerConfiguration serverConfiguration, boolean allowMissingChallenge) {
        super(serverConfiguration, allowMissingChallenge);
    }

    @Override
    protected void validateScope(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        super.validateScope(authorizationRequest, clientMetadata);
        if (!getConfiguration().getScopesSupported().containsAll(authorizationRequest.getScope())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Client requested scopes not supported by authorization server.", 400);
        }
    }

    @Override
    public TokenResponse process(TokenRequest tokenRequest) {
        if ("authorization_code".equals(tokenRequest.getGrantType())) {
            return super.process(tokenRequest);
        }
        return processPreAuthorizedTokenRequest(tokenRequest);
    }

    /**
     * Process Pre-authorized token request.  Validate request, lookup authorization, check tx_code and create token response.
     */
    protected TokenResponse processPreAuthorizedTokenRequest(TokenRequest tokenRequest) {
        final ClientMetadata clientMetadata;
        if (tokenRequest.isAuthenticatedRequest()) {
            clientMetadata = authenticateClient(tokenRequest);
        } else {
            if (getConfiguration().isPreAuthorizedGrantAnonymousAccessSupported()) {
                clientMetadata = handleUnauthenticatedClient(tokenRequest);
            } else {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Pre-authorized anonymous access not supported", 401);
            }
       }
        validate(tokenRequest, clientMetadata);
        getConfiguration().getAuditLogger().auditTokenRequest(tokenRequest);
        Authorization preAuthorization = getConfiguration().getCache().getAuthorization(tokenRequest.getPreAuthorizedCode());
        if (preAuthorization == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant does not exist.", 400);
        }
        if (!preAuthorization.isValidNow()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant has expired.", 400);
        }
        getConfiguration().getCache().removeAuthorization(tokenRequest.getPreAuthorizedCode());
        if (hasText(preAuthorization.getCodeChallenge()) && !validateCodeVerifier(tokenRequest.getTxCode(), preAuthorization.getCodeChallenge())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. Invalid transaction code.", 400);
        }
        if (tokenRequest.getDPoPHeader() != null) {
            String dpopJtk = validateDPoPProofAndGetDPoPJtk(tokenRequest.getDPoPHeader(), tokenRequest.getClientId(), getConfiguration().getTokenEndpoint());
            if (!hasText(dpopJtk)) { // dummy greia, ta vekk
                throw new OAuth2Exception(OAuth2Exception.INVALID_DPOP_PROOF, "Invalid DPop. The DPop header is invalid.", 400);
            }
            // TODO valider vidare
            preAuthorization.setDpopJkt(dpopJtk);
        }
        if (tokenRequest.hasResourceIndicator()) {
            preAuthorization.setAud(tokenRequest.getResource());
        }
        try {
            TokenResponse tokenResponse = createTokenResponse(preAuthorization);
            getConfiguration().getAuditLogger().auditTokenResponse(tokenResponse);
            return tokenResponse;
        } catch (Exception e) {
            throw new OAuth2Exception("internal_error", "The server failed to process the request", 500, e);
        }
    }

    /**
     * Handles OpenID4VCI unauthenticated token requests by creating a dummy client.
     *
     * @param tokenRequest pre-authorized token request
     * @return client metadata for unauthenticated client
     */
    private ClientMetadata handleUnauthenticatedClient(TokenRequest tokenRequest) {
        tokenRequest.clearAuthentication();
        ClientMetadata clientMetadata = ClientMetadata.builder().clientId("unauthenticated-pre-authorized").build();
        ClientAuthentication clientAuthentication = ClientAuthentication.builder().clientId(clientMetadata.getClientId()).tokenEndpointAuthMethod("none").build();
        tokenRequest.setAuthenticatedClientId(clientAuthentication.getClientId());
        getConfiguration().getAuditLogger().auditClientAuthentication(clientAuthentication);
        return clientMetadata;
    }

    private void validateApiKey(Map<String, List<String>> headers) {
        if (!headers.containsKey(X_API_KEY_HEADER)) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing API key header.", 401);
        }
        if (CollectionUtils.isEmpty(headers.get(X_API_KEY_HEADER))) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Empty API key header.", 401);
        }
        if (!Objects.equals(getConfiguration().getApiKey(), headers.get(X_API_KEY_HEADER).getFirst())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid API key.", 401);
        }
    }

    /**
     * Process pre-authorization request.  Create an authorization, store in cache and generate a response
     * with pre.authorization_code.
     */
    public PreAuthorizationResponse process(PreAuthorizationRequest preAuthorizationRequest, Map<String, List<String>> headers) {
        Map<String, List<String>> headersMap = MultiValuedMapUtils.caseInsensitiveMap(headers);
        validateApiKey(headersMap);
        validate(preAuthorizationRequest);
        Authorization preAuthorization = Authorization.builder()
                .sub(preAuthorizationRequest.getSub())
                .aud(preAuthorizationRequest.getAud())
                .scope(String.join(" ", preAuthorizationRequest.getScope()))
                .codeChallenge(preAuthorizationRequest.getTxCodeChallenge())
                .attribute("tx_id", preAuthorizationRequest.getTxId())
                .build();
        preAuthorization.setLifetimeSeconds(preAuthorizationRequest.getAuthorizationLifetimeSeconds() > 0 ? preAuthorizationRequest.getAuthorizationLifetimeSeconds() : getConfiguration().getAuthorizationLifetimeSeconds());
        String preAuthorizationCode = generateId();
        getConfiguration().getCache().putAuthorization(preAuthorizationCode, preAuthorization);
        getConfiguration().getAuditLogger().auditAuthorization(preAuthorization);
        PreAuthorizationResponse preAuthorizationResponse = PreAuthorizationResponse.builder()
                .preAuthorizedCode(preAuthorizationCode)
                .expiresInSeconds(preAuthorization.expiresInSeconds())
                .build();
        return preAuthorizationResponse;
    }

    public void validate(PreAuthorizationRequest preAuthorizationRequest) {
        if (!StringUtils.hasText(preAuthorizationRequest.getSub())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid pre-authorization. Invalid subject.", 400);
        }
        if (!StringUtils.hasText(preAuthorizationRequest.getAud())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid pre-authorization. Invalid audience.", 400);
        }
        if (preAuthorizationRequest.getScope().isEmpty()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid pre-authorization. Invalid scope.", 400);
        }
        if (!StringUtils.hasText(preAuthorizationRequest.getTxId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid pre-authorization. Invalid tx_id.", 400);
        }
        if (preAuthorizationRequest.getAuthorizationLifetimeSeconds() < 1) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid pre-authorization. Invalid authorization token lifetime.", 400);
        }
    }


}
