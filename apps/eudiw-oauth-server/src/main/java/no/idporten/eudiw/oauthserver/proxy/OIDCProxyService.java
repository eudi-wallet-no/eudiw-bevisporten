package no.idporten.eudiw.oauthserver.proxy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.Prompt;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.validators.identifier.PersonIdentifierValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.URI;
import java.security.Provider;
import java.util.Objects;

/**
 * Integrates with OIDC server.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class OIDCProxyService {

    private final OIDCProxyProperties oidcProxyProperties;

    /**
     * Create authorization request for OIDC server based on client authorization request.
     */
    public AuthorizationRequest createAuthorizationRequest(PushedAuthorizationRequest clientAuthorizationRequest, ProtocolVerifiers protocolVerifiers) {
        try {
            AuthenticationRequest.Builder requestBuilder = new AuthenticationRequest.Builder(
                    new ResponseType(ResponseType.Value.CODE),
                    new Scope("openid", "profile"),
                    oidcProxyProperties.getOidcClient().getClientID(),
                    oidcProxyProperties.getRedirectUri());
            requestBuilder
                    .endpointURI(oidcProxyProperties.getOidcIssuer().authorizationEndpoint())
                    .state(protocolVerifiers.state())
                    .nonce(protocolVerifiers.nonce())
                    .codeChallenge(protocolVerifiers.codeVerifier(), CodeChallengeMethod.S256)
                    .responseMode(ResponseMode.QUERY_JWT)
                    .prompt(new Prompt(Prompt.Type.LOGIN)
                    );
            AuthenticationRequest authenticationRequest = requestBuilder.build();
            authenticationRequest = pushAuthorizationRequest(authenticationRequest);
            return authenticationRequest;
        } catch (OIDCProxyException e) {
            throw e;
        } catch (Exception e) {
            throw new OIDCProxyException(OAuth2Exception.SERVER_ERROR, "Failed to create and push authorization request to OIDC server", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    protected AuthenticationRequest pushAuthorizationRequest(AuthenticationRequest authenticationRequest) throws Exception {
        ClientAuthentication clientAuthentication = clientAuthentication(oidcProxyProperties.getOidcIssuer(), oidcProxyProperties.getOidcClient());
        com.nimbusds.oauth2.sdk.PushedAuthorizationRequest pushedAuthorizationRequest = new com.nimbusds.oauth2.sdk.PushedAuthorizationRequest(oidcProxyProperties.getOidcIssuer().pushedAuthorizationRequestEndpoint(), clientAuthentication, authenticationRequest);
        PushedAuthorizationResponse pushedAuthorizationResponse = send(pushedAuthorizationRequest);
        if (pushedAuthorizationResponse.indicatesSuccess()) {
            PushedAuthorizationSuccessResponse successResponse = pushedAuthorizationResponse.toSuccessResponse();
            return new AuthenticationRequest.Builder(
                    successResponse.getRequestURI(),
                    authenticationRequest.getClientID())
                    .endpointURI(oidcProxyProperties.getOidcIssuer().authorizationEndpoint())
                    .build();
        } else {
            PushedAuthorizationErrorResponse pushedAuthorizationErrorResponse = pushedAuthorizationResponse.toErrorResponse();
            log.error("PAR request rejected by issuer {} with HTTP status {}: {}", oidcProxyProperties.getOidcIssuer().issuer(), pushedAuthorizationErrorResponse.getErrorObject().getHTTPStatusCode(), pushedAuthorizationErrorResponse.getErrorObject().toJSONObject().toJSONString());
            throw new OIDCProxyException(OAuth2Exception.SERVER_ERROR, "Failed to push request to OIDC server", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handle authorization response from OIDC server, fetch and validate tokens, and extract and create authorization data.
     */
    public Authorization handleAuthorizationResponse(PushedAuthorizationRequest clientAuthorizationRequest, MultiValueMap<String, String> parameters, ProtocolVerifiers protocolVerifiers) {
        try {
            com.nimbusds.oauth2.sdk.AuthorizationResponse authorizationResponse = com.nimbusds.oauth2.sdk.AuthorizationResponse.parse(URI.create("/callback"), parameters, oidcProxyProperties.getJarmValidator());
            if (!Objects.equals(protocolVerifiers.state(), authorizationResponse.getState())) {
                throw new OIDCProxyException(OAuth2Exception.INVALID_REQUEST, "Invalid state. State does not match state from original request.", HttpStatus.BAD_REQUEST);
            }
            if (authorizationResponse.indicatesSuccess()) {
                OIDCTokens oidcTokens = fetchTokens(authorizationResponse, protocolVerifiers);
                IDTokenClaimsSet idTokenClaimsSet = validateIDToken(oidcTokens.getIDToken(), protocolVerifiers);
                return buildAuthorization(clientAuthorizationRequest, oidcTokens, idTokenClaimsSet);
            } else {
                handleAuthorizationErrorResponse(authorizationResponse);
                throw new OIDCProxyException(OAuth2Exception.INVALID_REQUEST, "Invalid response from OIDC server", HttpStatus.BAD_REQUEST);
            }
        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            throw new OIDCProxyException(OAuth2Exception.SERVER_ERROR, "Failed to get tokens from OIDC server", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private OIDCTokens fetchTokens(AuthorizationResponse authorizationResponse, ProtocolVerifiers protocolVerifiers) throws Exception {
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(authorizationResponse.toSuccessResponse().getAuthorizationCode(), oidcProxyProperties.getRedirectUri(), protocolVerifiers.codeVerifier());
        final ClientAuthentication clientAuth = clientAuthentication(oidcProxyProperties.getOidcIssuer(), oidcProxyProperties.getOidcClient());
        TokenRequest tokenRequest = new TokenRequest(oidcProxyProperties.getOidcIssuer().tokenEndpoint(), clientAuth, codeGrant, (Scope) null);
        TokenResponse tokenResponse = send(tokenRequest);
        if (tokenResponse.indicatesSuccess()) {
            OIDCTokenResponse successResponse = (OIDCTokenResponse) tokenResponse.toSuccessResponse();
            return successResponse.getOIDCTokens();
        } else {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            log.warn("Error response from {}: {}", oidcProxyProperties.getOidcIssuer().tokenEndpoint(), errorResponse.toJSONObject().toJSONString());
            throw new OIDCProxyException(OAuth2Exception.SERVER_ERROR, "Failed to get tokens from OIDC server", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private IDTokenClaimsSet validateIDToken(JWT idToken, ProtocolVerifiers protocolVerifiers) throws Exception {
        IDTokenClaimsSet idTokenClaimsSet = oidcProxyProperties.getIdTokenValidator().validate(idToken, protocolVerifiers.nonce());
        String personIdentifier = idTokenClaimsSet.getStringClaim("pid");
        if (! PersonIdentifierValidator.isValid(personIdentifier)) {
            throw new OIDCProxyException(OAuth2Exception.SERVER_ERROR, "Invalid person identifier", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return idTokenClaimsSet;
    }

    private Authorization buildAuthorization(PushedAuthorizationRequest authorizationRequest, OIDCTokens oidcTokens, IDTokenClaimsSet idTokenClaimsSet) throws BadJOSEException, JOSEException {
        final String personIdentifier = idTokenClaimsSet.getStringClaim("pid");
        return Authorization.builder()
                .sub(personIdentifier)
                .acr(idTokenClaimsSet.getACR().getValue())
                .amr(idTokenClaimsSet.getAMR().getFirst().getValue())
                .attribute("xid", oidcTokens.getIDTokenString())
                .attribute("xat", oidcTokens.getAccessToken().getValue())
                .build();
    }

    protected TokenResponse send(com.nimbusds.oauth2.sdk.TokenRequest tokenRequest) throws ParseException {
        HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
        httpRequest.setConnectTimeout(oidcProxyProperties.getConnectTimeoutMillis());
        httpRequest.setReadTimeout(oidcProxyProperties.getReadTimeoutMillis());
        HTTPResponse httpResponse = null;
        try {
            httpResponse = httpRequest.send();
        } catch (IOException e) {
            throw new OIDCProxyException(OAuth2Exception.SERVER_ERROR, "Failed to get tokens from OIDC server", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        return OIDCTokenResponseParser.parse(httpResponse);
    }

    private void handleAuthorizationErrorResponse(AuthorizationResponse authorizationResponse) {
        AuthorizationErrorResponse errorResponse = authorizationResponse.toErrorResponse();
        String error = errorResponse.getErrorObject().getCode();
        if ("access_denied".equals(error)) {
            log.info("User cancel response from {}: {}", oidcProxyProperties.getOidcIssuer().issuer(), errorResponse.getErrorObject().toJSONObject().toJSONString());
            throw new OIDCProxyException(OAuth2Exception.INVALID_REQUEST, "User cancel", HttpStatus.BAD_REQUEST);
        }
        log.warn("Error response from {}: {}", oidcProxyProperties.getOidcIssuer().issuer(), errorResponse.getErrorObject().toJSONObject().toJSONString());
    }

    protected PushedAuthorizationResponse send(com.nimbusds.oauth2.sdk.PushedAuthorizationRequest pushedAuthorizationRequest) {
        HTTPRequest httpRequest = pushedAuthorizationRequest.toHTTPRequest();
        httpRequest.setConnectTimeout(oidcProxyProperties.getConnectTimeoutMillis());
        httpRequest.setReadTimeout(oidcProxyProperties.getReadTimeoutMillis());
        try {
            HTTPResponse httpResponse = httpRequest.send();
            return PushedAuthorizationResponse.parse(httpResponse);
        } catch (IOException | ParseException e) {
            throw new OIDCProxyException(OAuth2Exception.SERVER_ERROR, "Failed to parse response from OIDC server", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    protected ClientAuthentication clientAuthentication(OIDCIssuerProperties oidcIssuerProperties, OIDCClientProperties oidcClientProperties) throws Exception {
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(oidcClientProperties.getClientAuthenticationMethod())) {
            return new ClientSecretBasic(oidcClientProperties.getClientID(), oidcClientProperties.getClientSecret());
        }
        if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(oidcClientProperties.getClientAuthenticationMethod())) {
            return new ClientSecretPost(oidcClientProperties.getClientID(), oidcClientProperties.getClientSecret());
        }
        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(oidcClientProperties.getClientAuthenticationMethod())) {
            return new PrivateKeyJWT(oidcClientProperties.getClientID(), URI.create(oidcIssuerProperties.issuer().getValue()), JWSAlgorithm.RS256, oidcClientProperties.getKeyProvider().privateKey(), oidcClientProperties.getKeyProvider().getAlias(), (Provider) null);
        }
        throw new OIDCProxyException(OAuth2Exception.SERVER_ERROR, "Unsupported client authentication method", HttpStatus.BAD_REQUEST);

    }

}
