package no.idporten.sdk.oidcserver;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.*;
import no.idporten.sdk.oidcserver.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

import static no.idporten.sdk.oidcserver.util.StringUtils.hasText;

@Slf4j
public class OpenIDConnectIntegrationBase implements OpenIDConnectIntegration {

    private final OpenIDConnectSdkConfiguration sdkConfiguration;

    public OpenIDConnectIntegrationBase(OpenIDConnectSdkConfiguration sdkConfiguration) {
        sdkConfiguration.validate();
        this.sdkConfiguration = sdkConfiguration;
    }

    @Override
    public JWKSet getPublicJWKSet() {
        return new JWKSet(sdkConfiguration.getJwk()).toPublicJWKSet();
    }

    @Override
    public OpenIDProviderMetadataResponse getOpenIDProviderMetadata() {
        return OpenIDProviderMetadataResponse.builder()
                .issuer(sdkConfiguration.getIssuer())
                .pushedAuthorizationRequestEndpoint(sdkConfiguration.getPushedAuthorizationRequestEndpoint())
                .authorizationEndpoint(sdkConfiguration.getAuthorizationEndpoint())
                .tokenEndpoint(sdkConfiguration.getTokenEndpoint())
                .userinfoEndpoint(sdkConfiguration.getUserinfoEndpoint())
                .jwksUri(sdkConfiguration.getJwksUri())
                .grantTypesSupported(sdkConfiguration.getGrantTypesSupported())
                .acrValuesSupported(sdkConfiguration.getAcrValues())
                .uiLocalesSupported(sdkConfiguration.getUiLocales())
                .codeChallengeMethodsSupported(sdkConfiguration.getCodeChallengeMethodsSupported())
                .responseModesSupported(sdkConfiguration.getResponseModes())
                .scopesSupported(sdkConfiguration.getScopesSupported())
                .claimsSupported(sdkConfiguration.getClaimsSupported())
                .authorizationDetailsTypesSupported(sdkConfiguration.getAuthorizationDetailsTypesSupported())
                .requirePushedAuthorizationRequests(sdkConfiguration.isRequirePushedAuthorizationRequests())
                .idTokenSigningAlgValueSupported(sdkConfiguration.getDefaultSigningAlgorithm().getName())
                .authorizationSigningAlgValueSupported(sdkConfiguration.getDefaultSigningAlgorithm().getName())
                .tokenEndpointAuthSigningAlgValuesSupported(sdkConfiguration.getTokenEndpointAuthSigningAlgValuesSupported().stream().map(Algorithm::getName).toList())
                .authorizationResponseIssParameterSupported(sdkConfiguration.isAuthorizationResponseIssParameterSupported())
                .build();
    }

    @Override
    public void validate(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (authorizationRequest == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Empty or missing request.", 400);
        }
        if (clientMetadata == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Unknown client.", 400);
        }
        validateClientId(authorizationRequest, clientMetadata);
        validateRedirectUri(authorizationRequest, clientMetadata);
        validateResponseType(authorizationRequest, clientMetadata);
        validateCodeChallenge(authorizationRequest, clientMetadata);
        validateScope(authorizationRequest, clientMetadata);
        validateUiLocales(authorizationRequest, clientMetadata);
        validateResponseMode(authorizationRequest, clientMetadata);
        validateState(authorizationRequest, clientMetadata);
        validateNonce(authorizationRequest, clientMetadata);
        validateAuthorizationDetails(authorizationRequest, clientMetadata);
        validateResource(authorizationRequest, clientMetadata);
    }

    protected void validateClientId(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (!hasText(authorizationRequest.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter client_id.", 400);
        }
        if (!Objects.equals(authorizationRequest.getClientId(), clientMetadata.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter client_id. The request was pushed by another client.", 400);
        }
    }

    protected void validateRedirectUri(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (!hasText(authorizationRequest.getRedirectUri())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter redirect_uri.", 400);
        }
        if (!clientMetadata.getRedirectUris().contains(authorizationRequest.getRedirectUri())) {
            log.info("Invalid redirect_uri [{}] for client [{}]", authorizationRequest.getRedirectUri(), clientMetadata.getClientId());
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter redirect_uri. Not registered on client.", 400);
        }
    }

    @SuppressWarnings("unused")
    protected void validateResponseType(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (!"code".equals(authorizationRequest.getResponseType())) {
            throw new OAuth2Exception(OAuth2Exception.UNSUPPORTED_RESPONSE_TYPE, "Invalid parameter response_type. Only authorization code flow is supported.", 400);
        }
    }

    @SuppressWarnings("unused")
    protected void validateCodeChallenge(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (hasText(authorizationRequest.getCodeChallengeMethod()) && !sdkConfiguration.getCodeChallengeMethodsSupported().contains(authorizationRequest.getCodeChallengeMethod())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter code_challenge_method.", 400);
        }
        if (sdkConfiguration.isRequirePkce() && !hasText(authorizationRequest.getCodeChallenge())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter code_challenge. PKCE is required.", 400);
        }
    }

    protected void validateScope(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (authorizationRequest.getScope().isEmpty()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_SCOPE, "No scopes requested.", 400);
        }
    }


    @SuppressWarnings("unused")
    protected void validateUiLocales(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        // TODO
//        if (authorizationRequest.getUiLocales().isEmpty()) {
//            authorizationRequest.setResolvedUiLocale(sdkConfiguration.getUiLocales().get(0));
//        } else {
//            authorizationRequest.setResolvedUiLocale(
//                    authorizationRequest.getUiLocales().stream()
//                            .filter(uiLocale -> sdkConfiguration.getUiLocales().contains(uiLocale))
//                            .findFirst()
//                            .orElse(sdkConfiguration.getUiLocales().get(0)));
//        }
    }

    @SuppressWarnings("unused")
    protected void validateResponseMode(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (hasText(authorizationRequest.getResponseMode())) {
            if (!sdkConfiguration.getResponseModes().contains(authorizationRequest.getResponseMode())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter response_mode. Unsupported response mode.", 400);
            }
            authorizationRequest.setResolvedResponseMode(authorizationRequest.getResponseMode());
        } else {
            authorizationRequest.setResolvedResponseMode("query");
        }
    }

    @SuppressWarnings("unused")
    protected void validateState(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (hasText(authorizationRequest.getState()) && !authorizationRequest.getState().matches("^[\\x20-\\x7E]+$")) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter state.", 400);
        }
    }

    @SuppressWarnings("unused")
    protected void validateNonce(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (hasText(authorizationRequest.getNonce()) && !authorizationRequest.getNonce().matches("^[\\x20-\\x7E]+$")) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter nonce.", 400);
        }
    }

    @SuppressWarnings("unused")
    protected void validateAuthorizationDetails(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (authorizationRequest.getAuthorizationDetails() != null && !authorizationRequest.getAuthorizationDetails().isEmpty()) {
            for (AuthorizationDetail authorizationDetail : authorizationRequest.getAuthorizationDetails()) {
                if (!hasText(authorizationDetail.getType())) {
                    throw new OAuth2Exception(OAuth2Exception.INVALID_AUTHORIZATION_DETAILS, "Invalid parameter authorization_details. Must contain type.", 400);
                }
                if (!getSDKConfiguration().supportsAuthorizationDetailsType(authorizationDetail.getType())) {
                    throw new OAuth2Exception(OAuth2Exception.INVALID_AUTHORIZATION_DETAILS, "Unsupported authorization_details type.", 400);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    protected void validateResource(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (authorizationRequest.getResource() != null) {
            final URI uri;
            try {
                uri = new URI(authorizationRequest.getResource());
            } catch (URISyntaxException e) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_TARGET, "Invalid parameter resource.", 400);
            }
            if (! uri.isAbsolute()) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_TARGET, "Invalid parameter resource. Must be absolute.", 400);
            }
            if (StringUtils.hasText(uri.getQuery())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_TARGET, "Invalid parameter resource.  Cannot contain query params.", 400);
            }
            if (StringUtils.hasText(uri.getFragment())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_TARGET, "Invalid parameter resource.  Cannot contain fragments.", 400);
            }
        }
    }

    /**
     * Processes the pushed authorization request and produces a response.
     * <p>
     * * checks client authentication (override {@link #authenticateClient(AuthenticatedRequest)}  to modify default behaviour)
     * * validates OIDC/Oauth2 parameters (override {@link #validate(PushedAuthorizationRequest, ClientMetadata)} to
     * modify default behaviour)
     *
     * @param authorizationRequest authorization request
     * @return pushed authorization response
     */
    @Override
    public final PushedAuthorizationResponse process(PushedAuthorizationRequest authorizationRequest) {
        ClientMetadata clientMetadata = authenticateClient(authorizationRequest);
        validate(authorizationRequest, clientMetadata);
        sdkConfiguration.getAuditLogger().auditPushedAuthorizationRequest(authorizationRequest);
        PushedAuthorizationResponse pushedAuthorizationResponse = Objects.requireNonNull(createResponse(authorizationRequest));
        sdkConfiguration.getAuditLogger().auditPushedAuthorizationResponse(pushedAuthorizationResponse);
        return pushedAuthorizationResponse;
    }

    /**
     * Extension point for custom pushed authorization request handling.
     *
     * @param authorizationRequest the pushed authorization request
     * @return PushedAuthorizationResponse
     */
    protected PushedAuthorizationResponse createResponse(PushedAuthorizationRequest authorizationRequest) {
        return createPushedAuthorizationResponse(authorizationRequest);
    }

    /**
     * Creates a pushed authorization response to the pushed authorization request.  Clients must redirect the browser
     * to the authorization endpoint.
     * @param authorizationRequest client authz request
     * @return pushed authorization response with request_uri
     */
    protected final PushedAuthorizationResponse createPushedAuthorizationResponse(PushedAuthorizationRequest authorizationRequest) {
        String requestUri = createRequestUri();
        authorizationRequest.setLifetimeSeconds(sdkConfiguration.getAuthorizationRequestLifetimeSeconds());
        sdkConfiguration.getCache().putAuthorizationRequest(requestUri, authorizationRequest);
        return PushedAuthorizationResponse.builder().expiresIn(authorizationRequest.expiresInSeconds()).requestUri(requestUri).build();
    }

    /**
     * Creates a direct pushed authorization response to the pushed authorization request.  Clients must not redirect to
     * the authorization endpoint.  They must handle the custom response containing tokens.
     * @param authorizationRequest client authz request
     * @param authorization        base for token creation
     * @return direct pushed authorization response with tokens
     */
    @SneakyThrows
    protected final PushedAuthorizationResponse createDirectPushedAuthorizationResponse(PushedAuthorizationRequest authorizationRequest, Authorization authorization) {
        authorization.setLifetimeSeconds(sdkConfiguration.getAuthorizationLifetimeSeconds());
        authorization.setAud(authorizationRequest.getClientId());
        authorization.setNonce(authorizationRequest.getNonce());
        if (!hasText(authorization.getAcr())) {
            authorization.setAcr(authorizationRequest.getResolvedAcrValue());
        }
        sdkConfiguration.getAuditLogger().auditAuthorization(authorization);
        TokenResponse tokenResponse = createTokenResponse(authorization);
        return DirectPushedAuthorizationResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .state(authorizationRequest.getState())
                .expiresIn(sdkConfiguration.getAccessTokenLifetimeSeconds())
                .build();
    }

    @Override
    public ClientMetadata authenticateClient(AuthenticatedRequest authenticatedRequest) {
        if (!authenticatedRequest.isAuthenticatedRequest()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Missing client authentication.", 401);
        }
        if (authenticatedRequest.hasMoreThanOneClientAuthMethod()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Multiple client authentications.", 401);
        }
        final ClientMetadata clientMetadata;
        final ClientAuthentication clientAuthentication;
        if (authenticatedRequest.isClientSecretPost()) {
            clientMetadata = authenticateClient(authenticatedRequest.getClientId(), authenticatedRequest.getClientSecret());
            clientAuthentication = ClientAuthentication.builder().clientId(clientMetadata.getClientId()).tokenEndpointAuthMethod("client_secret_post").build();
        } else if (authenticatedRequest.isClientSecretJwt()) {
            clientMetadata = authenticateClientByJwt(authenticatedRequest.getClientAssertion());
            clientAuthentication = ClientAuthentication.builder().clientId(clientMetadata.getClientId()).tokenEndpointAuthMethod("client_secret_jwt").build();
        } else if (authenticatedRequest.isClientSecretBasic()) {
            final String clientId;
            final String clientSecret;
            try {
                String httpBasicAuthorizationHeader = authenticatedRequest.getAuthorizationHeader();
                final String encodedCredentials = httpBasicAuthorizationHeader.substring(httpBasicAuthorizationHeader.lastIndexOf("Basic ") + 6);
                final String decodedCredentials = new String(java.util.Base64.getDecoder().decode(encodedCredentials));
                String[] credentials = decodedCredentials.split(":");
                clientId = credentials[0];
                clientSecret = credentials[1];
            } catch (Exception e) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Invalid Authorization header.", 401, e);
            }
            clientMetadata = authenticateClient(clientId, clientSecret);
            clientAuthentication = ClientAuthentication.builder().clientId(clientMetadata.getClientId()).tokenEndpointAuthMethod("client_secret_basic").build();
        } else if (authenticatedRequest.isNone()) {
            if (authenticatedRequest instanceof PushedAuthorizationRequest) {
                clientMetadata = ClientMetadata.builder().clientId(authenticatedRequest.getClientId())
                        .redirectUri(((PushedAuthorizationRequest) authenticatedRequest).getRedirectUri()).build();
            } else {
                clientMetadata = ClientMetadata.builder().clientId(authenticatedRequest.getClientId()).build();
            }
            clientAuthentication = ClientAuthentication.builder().clientId(clientMetadata.getClientId()).tokenEndpointAuthMethod("none").build();
        } else {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unknown client authentication method.", 401);
        }
        if (hasText(authenticatedRequest.getClientId()) && !Objects.equals(authenticatedRequest.getClientId(), clientMetadata.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Client authentication does not match parameter client_id.", 401);
        }
        authenticatedRequest.clearAuthentication();
        authenticatedRequest.setAuthenticatedClientId(clientAuthentication.getClientId());
        sdkConfiguration.getAuditLogger().auditClientAuthentication(clientAuthentication);
        return clientMetadata;
    }

    protected ClientMetadata authenticateClient(String clientId, String clientSecret) {
        final ClientMetadata clientMetadata = sdkConfiguration.findClient(clientId);
        if (clientMetadata == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unknown client.", 401);
        }
        if (!(Objects.equals(clientMetadata.getClientId(), clientId)
                && Objects.equals(clientMetadata.getClientSecret(), clientSecret))) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication.", 401);

        }
        return clientMetadata;
    }

    protected ClientMetadata authenticateClientByJwt(final String jwt) {
        try {
            final SignedJWT signedJWT = SignedJWT.parse(jwt);
            if (!sdkConfiguration.getTokenEndpointAuthSigningAlgValuesSupported().contains(signedJWT.getHeader().getAlgorithm())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unsupported JWT signing algorithm.", 401);
            }
            if (signedJWT.getJWTClaimsSet().getAudience() == null || signedJWT.getJWTClaimsSet().getAudience().isEmpty()) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Missing JWT audience.", 401);
            }
            if (signedJWT.getJWTClaimsSet().getAudience().size() != 1) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unique JWT audience required.", 401);
            }
            if (!signedJWT.getJWTClaimsSet().getAudience().contains(sdkConfiguration.getIssuer().toString())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unknown JWT audience.", 401);
            }
            final ClientMetadata clientMetadata = sdkConfiguration.findClient(signedJWT.getJWTClaimsSet().getSubject());
            JWSVerifier jwsVerifier = new MACVerifier(clientMetadata.getClientSecret());
            if (!signedJWT.verify(jwsVerifier)) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Invalid JWT signature.", 401);
            }
            if (!StringUtils.hasText(signedJWT.getJWTClaimsSet().getJWTID())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Missing JWT jti claim.", 401);
            }
            if (signedJWT.getJWTClaimsSet().getExpirationTime() == null) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Missing JWT exp claim.", 401);
            }
            if (signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. JWT expired.", 401);
            }
            return clientMetadata;
        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Assertion processing failed.", 401, e);
        }
    }

    @Override
    public void validate(AuthorizationRequest authorizationRequest) throws OAuth2Exception {
        if (authorizationRequest == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid request. Empty request.", 400);
        }
        validateRequestUri(authorizationRequest);
        validateClientId(authorizationRequest);
    }

    protected void validateRequestUri(AuthorizationRequest authorizationRequest) {
        if (!hasText(authorizationRequest.getRequestUri())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter request_uri.", 400);
        }
        if (!authorizationRequest.getRequestUri().matches("^urn:%s:.*$".formatted(sdkConfiguration.getInternalId()))) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter request_uri.", 400);
        }
    }

    protected void validateClientId(AuthorizationRequest authorizationRequest) {
        if (sdkConfiguration.isDisableClientIdCheckOnPARAuthorizationRequests() && !hasText(authorizationRequest.getClientId())) {
            return;
        }
        if (!hasText(authorizationRequest.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter client_id.", 400);
        }
    }

    protected String createRequestUri() {
        return "urn:%s:%s".formatted(sdkConfiguration.getInternalId(), generateId());
    }

    @Override
    public PushedAuthorizationRequest process(AuthorizationRequest authorizationRequest) {
        validate(authorizationRequest);
        final PushedAuthorizationRequest pushedAuthorizationRequest = sdkConfiguration.getCache().getAuthorizationRequest(authorizationRequest.getRequestUri());
        if (pushedAuthorizationRequest == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter request_uri. request_uri does not exist.", 400);
        }
        if (!pushedAuthorizationRequest.isValidNow()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter request_uri. request_uri has expired.", 400);
        }
        if (hasText(authorizationRequest.getClientId()) && !authorizationRequest.getClientId().equals(pushedAuthorizationRequest.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter client_id. The request was pushed by another client.", 400);
        }
        sdkConfiguration.getCache().removeAuthorizationRequest(authorizationRequest.getRequestUri());
        sdkConfiguration.getAuditLogger().auditAuthorizationRequest(authorizationRequest);
        return pushedAuthorizationRequest;
    }

    @Override
    public AuthorizationResponse authorize(PushedAuthorizationRequest pushedAuthorizationRequest, Authorization authorization) {
        String code = generateId();
        authorization.setNonce(pushedAuthorizationRequest.getNonce());
        authorization.setCodeChallenge(pushedAuthorizationRequest.getCodeChallenge());
        authorization.setLifetimeSeconds(sdkConfiguration.getAuthorizationLifetimeSeconds());
        authorization.setClientId(pushedAuthorizationRequest.getClientId());
        authorization.setAud(pushedAuthorizationRequest.getResource());
        authorization.setScope(String.join(" ", pushedAuthorizationRequest.getScope()));
        if (!hasText(authorization.getAcr())) {
            authorization.setAcr(pushedAuthorizationRequest.getResolvedAcrValue());
        }
        validateAuthorization(authorization);
        sdkConfiguration.getCache().putAuthorization(code, authorization);
        sdkConfiguration.getAuditLogger().auditAuthorization(authorization);
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .redirectUri(pushedAuthorizationRequest.getRedirectUri())
                .aud(pushedAuthorizationRequest.getClientId())
                .iss(sdkConfiguration.isAuthorizationResponseIssParameterSupported() ? sdkConfiguration.getIssuer().toString() : null)
                .responseMode(pushedAuthorizationRequest.getResolvedResponseMode())
                .code(code)
                .state(pushedAuthorizationRequest.getState())
                .build();
        sdkConfiguration.getAuditLogger().auditAuthorizationResponse(authorizationResponse);

        return authorizationResponse;
    }

    @Override
    public AuthorizationResponse errorResponse(PushedAuthorizationRequest pushedAuthorizationRequest, String error, String errorDescription) {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .redirectUri(pushedAuthorizationRequest.getRedirectUri())
                .aud(pushedAuthorizationRequest.getClientId())
                .iss(sdkConfiguration.isAuthorizationResponseIssParameterSupported() ? sdkConfiguration.getIssuer().toString() : null)
                .responseMode(pushedAuthorizationRequest.getResolvedResponseMode())
                .error(error)
                .errorDescription(errorDescription)
                .state(pushedAuthorizationRequest.getState())
                .build();
        sdkConfiguration.getAuditLogger().auditAuthorizationResponse(authorizationResponse);
        return authorizationResponse;
    }

    @Override
    public AuthorizationResponse panicErrorResponse(ClientMetadata clientMetadata, String error, String errorDescription) {
        Objects.requireNonNull(clientMetadata);
        Objects.requireNonNull(error);
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .redirectUri(clientMetadata.getRedirectUris().getFirst())
                .aud(clientMetadata.getClientId())
                .iss(sdkConfiguration.isAuthorizationResponseIssParameterSupported() ? sdkConfiguration.getIssuer().toString() : null)
                .responseMode("query.jwt")
                .error(error)
                .errorDescription(errorDescription)
                .build();
        sdkConfiguration.getAuditLogger().auditAuthorizationResponse(authorizationResponse);
        return authorizationResponse;
    }

    /**
     * Processes the token request and creates a token response.  Builds and signs tokens.
     *
     * @param tokenRequest OAuth2 token request
     * @return token response
     */
    @Override
    public TokenResponse process(TokenRequest tokenRequest) {
        ClientMetadata clientMetadata = authenticateClient(tokenRequest);
        validate(tokenRequest, clientMetadata);
        sdkConfiguration.getAuditLogger().auditTokenRequest(tokenRequest);
        Authorization authorization = sdkConfiguration.getCache().getAuthorization(tokenRequest.getCode());
        if (authorization == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant does not exist.", 400);
        }
        if (!authorization.isValidNow()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant has expired.", 400);
        }
        if (!Objects.equals(clientMetadata.getClientId(), authorization.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant is not issued to authenticated client.", 400);
        }
        if ((getSDKConfiguration().isRequirePkce() || hasText(authorization.getCodeChallenge())) && !validateCodeVerifier(tokenRequest.getCodeVerifier(), authorization.getCodeChallenge())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. Invalid code_verifier.", 400);
        }
        sdkConfiguration.getCache().removeAuthorization(tokenRequest.getCode());
        try {
            TokenResponse tokenResponse = createTokenResponse(authorization);
            if (sdkConfiguration.getUserinfoEndpoint() != null) {
                authorization = authorization.toBuilder().build();
                authorization.setLifetimeSeconds(sdkConfiguration.getAccessTokenLifetimeSeconds());
                sdkConfiguration.getCache().putAccessTokenAndAuthorization(tokenResponse.getAccessToken(), authorization);
            }
            sdkConfiguration.getAuditLogger().auditTokenResponse(tokenResponse);
            return tokenResponse;
        } catch (Exception e) {
            throw new OAuth2Exception("internal_error", "The server failed to process the request", 500, e);
        }
    }

    private void validateAuthorization(Authorization authorization) {
        Objects.requireNonNull(authorization);
        Objects.requireNonNull(authorization.getAud(), "authorization must have an audience");
        Objects.requireNonNull(authorization.getSub(), "authorization must have a sub");
    }

    protected final TokenResponse createTokenResponse(Authorization authorization) throws JOSEException {
        validateAuthorization(authorization);
        boolean isOpenIDConnect = authorization.getScope().contains("openid");
        return TokenResponse.builder()
                .idToken(isOpenIDConnect ? createIDToken(authorization) : null)
                .accessToken(createAccessToken(authorization))
                .expiresInSeconds(sdkConfiguration.getAccessTokenLifetimeSeconds())
                .build();
    }

    private String createIDToken(Authorization authorization) throws JOSEException {
        String idToken;
        JWTClaimsSet.Builder idTokenClaimsSetBuilder = new JWTClaimsSet.Builder()
                .jwtID(generateId())
                .issuer(sdkConfiguration.getIssuer().toString())
                .audience(authorization.getClientId())
                .expirationTime(new Date(new Date().getTime() + (sdkConfiguration.getIdTokenLifetimeSeconds() * 1000L)))
                .issueTime(new Date())
                .claim("auth_time", new Date().getTime() / 1000)
                .claim("nonce", authorization.getNonce())
                .subject(authorization.getSub())
                .claim("acr", authorization.getAcr());
        if (hasText(authorization.getAmr())) {
            idTokenClaimsSetBuilder.claim("amr", authorization.getAmr().split(",\\s*"));
        }
        authorization.getAttributes().forEach(idTokenClaimsSetBuilder::claim);
        idToken = signJwt(idTokenClaimsSetBuilder.build());
        return idToken;
    }

    private String createAccessToken(Authorization authorization) throws JOSEException {
        String accessToken;
        JWTClaimsSet.Builder accessTokenClaimsSetBuilder = new JWTClaimsSet.Builder()
                .jwtID(generateId())
                .issuer(sdkConfiguration.getIssuer().toString())
                .audience(authorization.getAud())
                .claim("client_id", authorization.getClientId())
                .claim("scope", authorization.getScope())
                .expirationTime(new Date(new Date().getTime() + (sdkConfiguration.getAccessTokenLifetimeSeconds() * 1000L)))
                .issueTime(new Date())
                .subject(authorization.getSub());
        authorization.getAttributes().forEach(accessTokenClaimsSetBuilder::claim);
        accessToken = signJwt("at+JWT", accessTokenClaimsSetBuilder.build());
        return accessToken;
    }


    @Override
    public UserInfoResponse process(UserInfoRequest userInfoRequest) {
        validate(userInfoRequest);
        sdkConfiguration.getAuditLogger().auditUserInfoRequest(userInfoRequest);
        Authorization authorization = sdkConfiguration.getCache().getAuthorizationByAccessToken(userInfoRequest.getBearerToken());
        if (authorization == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_TOKEN, "Invalid token. The token does not exist.", 401);
        }
        if (!authorization.isValidNow()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_TOKEN, "Invalid token. The token has expired.", 401);
        }
        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                .sub(authorization.getSub())
                .build();
        sdkConfiguration.getAuditLogger().auditUserInfoResponse(userInfoResponse);
        return userInfoResponse;
    }

    @Override
    public void validate(UserInfoRequest userInfoRequest) {
        if (!hasText(userInfoRequest.getAuthorizationHeader())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Missing authorization header.", 400);
        }
        if (!hasText(userInfoRequest.getBearerToken())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_TOKEN, "Missing bearer token in authorization header.", 401);
        }
    }

    public ClientResponse createClientResponse(AuthorizationResponse authorizationResponse) {
        if (authorizationResponse.isQuery()) {
            return new RedirectedResponse(authorizationResponse.getRedirectUri(), authorizationResponse.toResponseParameters());
        }
        if (authorizationResponse.isQueryJwt()) {
            try {
                JWTClaimsSet.Builder jwtClaimsSetBuilder = new JWTClaimsSet.Builder()
                        .jwtID(generateId())
                        .audience(authorizationResponse.getAud())
                        .issuer(sdkConfiguration.getIssuer().toString())
                        .expirationTime(new Date(new Date().getTime() + (sdkConfiguration.getAuthorizationLifetimeSeconds() * 1000L)))
                        .issueTime(new Date());
                authorizationResponse.toResponseParameters().forEach(jwtClaimsSetBuilder::claim);
                return new RedirectedResponse(authorizationResponse.getRedirectUri(), Map.of("response", signJwt(jwtClaimsSetBuilder.build())));
            } catch (JOSEException e) {
                throw new OAuth2Exception(OAuth2Exception.SERVER_ERROR, "Failed to send signed response", 500, e);
            }
        } else {
            return new FormPostResponse(authorizationResponse.getRedirectUri(), authorizationResponse.toResponseParameters());
        }
    }

    @Override
    public void validate(TokenRequest tokenRequest, ClientMetadata clientMetadata) {
        if (tokenRequest == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Empty or missing request.", 400);
        }
        if (!hasText(tokenRequest.getGrantType()) || !sdkConfiguration.getGrantTypesSupported().contains(tokenRequest.getGrantType())) {
            throw new OAuth2Exception(OAuth2Exception.UNSUPPORTED_GRANT_TYPE, "Invalid parameter grant_type. Supported: " + sdkConfiguration.getGrantTypesSupported(), 400);
        }
        if (!hasText(tokenRequest.getCode())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter code.", 400);
        }
        if (!hasText(tokenRequest.getRedirectUri()) && clientMetadata.getRedirectUris().size() > 1) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter redirect_uri.", 400);
        }
        if (!hasText(tokenRequest.getCodeVerifier()) && getSDKConfiguration().isRequirePkce()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter code_verifier. PKCE is required.", 400);
        }
        if (hasText(tokenRequest.getCodeVerifier()) && !tokenRequest.getCodeVerifier().matches("^[A-Za-z0-9\\-._~]{43,128}$")) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter code_verifier.", 400);
        }
    }

    protected boolean validateCodeVerifier(String codeVerifier, String codeChallenge) {
        if (!hasText(codeVerifier)) {
            return false;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm SHA-256 not found", e);
        }
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        byte[] decode = Base64.getUrlDecoder().decode(codeChallenge);
        return Arrays.equals(hash, decode);
    }

    @Override
    public ClientMetadata findClient(String clientId) {
        return sdkConfiguration.findClient(clientId);
    }

    @Override
    public OpenIDConnectSdkConfiguration getSDKConfiguration() {
        return sdkConfiguration;
    }

    protected String generateId() {
        byte[] bytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    protected String signJwt(JWTClaimsSet jwtClaimsSet) throws JOSEException {
        return signJwt(null, jwtClaimsSet);
    }

    protected String signJwt(String type, JWTClaimsSet jwtClaimsSet) throws JOSEException {
        JWK jwk = sdkConfiguration.getJwk();
        boolean isEC = KeyType.EC.equals(jwk.getKeyType());
        JWSSigner signer = isEC ? new ECDSASigner(jwk.toECKey()) : new RSASSASigner(jwk.toRSAKey());
        JWSAlgorithm signingAlgorithm = isEC ? JWSAlgorithm.ES256 : JWSAlgorithm.RS256;
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader
                        .Builder(signingAlgorithm)
                        .type(StringUtils.hasText(type) ? new JOSEObjectType(type) : null)
                        .keyID(sdkConfiguration.getJwk().getKeyID())
                        .build(),
                jwtClaimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

}
