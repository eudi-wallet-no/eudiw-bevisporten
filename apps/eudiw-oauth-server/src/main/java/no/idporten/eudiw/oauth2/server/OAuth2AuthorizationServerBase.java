package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.util.X509CertChainUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.dpop.JWKThumbprintConfirmation;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPIssuer;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPTokenRequestVerifier;
import com.nimbusds.oauth2.sdk.dpop.verifiers.InvalidDPoPProofException;
import com.nimbusds.openid.connect.sdk.Nonce;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.*;
import no.idporten.eudiw.oauth2.server.util.JsonObjectBuilder;
import no.idporten.eudiw.oauth2.server.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;

import static no.idporten.eudiw.oauth2.server.util.StringUtils.hasText;

@Slf4j
public class OAuth2AuthorizationServerBase implements OAuth2AuthorizationServer {

    private final OAuth2ServerConfiguration serverConfiguration;

    public OAuth2AuthorizationServerBase(OAuth2ServerConfiguration serverConfiguration) {
        serverConfiguration.validate();
        this.serverConfiguration = serverConfiguration;
    }


    @Override
    public JWKSet getPublicJWKSet() {
        return new JWKSet(serverConfiguration.getJwk()).toPublicJWKSet();
    }

    @Override
    public OpenIDProviderMetadataResponse getOpenIDProviderMetadata() {
        return OpenIDProviderMetadataResponse.builder()
                .issuer(serverConfiguration.getIssuer())
                .pushedAuthorizationRequestEndpoint(serverConfiguration.getPushedAuthorizationRequestEndpoint())
                .authorizationEndpoint(serverConfiguration.getAuthorizationEndpoint())
                .tokenEndpoint(serverConfiguration.getTokenEndpoint())
                .userinfoEndpoint(serverConfiguration.getUserinfoEndpoint())
                .challengeEndpoint(serverConfiguration.getChallengeEndpoint())
                .jwksUri(serverConfiguration.getJwksUri())
                .grantTypesSupported(serverConfiguration.getGrantTypesSupported())
                .acrValuesSupported(serverConfiguration.getAcrValues())
                .uiLocalesSupported(serverConfiguration.getUiLocales())
                .codeChallengeMethodsSupported(serverConfiguration.getCodeChallengeMethodsSupported())
                .responseModesSupported(serverConfiguration.getResponseModes())
                .scopesSupported(serverConfiguration.getScopesSupported())
                .claimsSupported(serverConfiguration.getClaimsSupported())
                .authorizationDetailsTypesSupported(serverConfiguration.getAuthorizationDetailsTypesSupported())
                .requirePushedAuthorizationRequests(serverConfiguration.isRequirePushedAuthorizationRequests())
                .idTokenSigningAlgValueSupported(serverConfiguration.getDefaultSigningAlgorithm().getName())
                .authorizationSigningAlgValueSupported(serverConfiguration.getDefaultSigningAlgorithm().getName())
                .authorizationResponseIssParameterSupported(serverConfiguration.isAuthorizationResponseIssParameterSupported())
                .preAuthorizedGrantAnonymousAccessSupported(serverConfiguration.isPreAuthorizedGrantAnonymousAccessSupported())
                .dpopSigningAlgValuesSupported(serverConfiguration.getDPopSigningAlgValuesSupported().stream().map(Algorithm::getName).toList())
                .dpopBoundAccessTokens(serverConfiguration.isDPopBoundAccessTokens())
                .clientAttestationSigningAlgValuesSupported(serverConfiguration.getClientAttestationSigningAlgValuesSupported().stream().map(Algorithm::getName).toList())
                .clientAttestationPopSigningAlgValuesSupported(serverConfiguration.getClientAttestationPoPSigningAlgValuesSupported().stream().map(Algorithm::getName).toList())
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
        validateIssuerState(authorizationRequest, clientMetadata);
        validateDPoP(authorizationRequest, clientMetadata);
    }

    protected void validateDPoP(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        // TODO implement DPoP validation and correct error messages, see RFC.
        if (authorizationRequest.getDPoPHeader() != null) {
            String dpopJtk = validateDPoPProofAndGetDPoPJtk(authorizationRequest.getDPoPHeader(), authorizationRequest.getClientId(), serverConfiguration.getPushedAuthorizationRequestEndpoint());
            if(authorizationRequest.getDpopJkt() != null && !authorizationRequest.getDpopJkt().equals(dpopJtk)) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_DPOP_PROOF, "Invalid parameter dpop_jkt and header DPoP, must be equal to each other.", 400);
            }
            authorizationRequest.setResolvedDpopJkt(dpopJtk);
        } else if(authorizationRequest.getDpopJkt() != null) {
            authorizationRequest.setResolvedDpopJkt(authorizationRequest.getDpopJkt());
        }
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
        if (hasText(authorizationRequest.getCodeChallengeMethod()) && !serverConfiguration.getCodeChallengeMethodsSupported().contains(authorizationRequest.getCodeChallengeMethod())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter code_challenge_method.", 400);
        }
        if (serverConfiguration.isRequirePkce() && !hasText(authorizationRequest.getCodeChallenge())) {
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
//            authorizationRequest.setResolvedUiLocale(serverConfiguration.getUiLocales().get(0));
//        } else {
//            authorizationRequest.setResolvedUiLocale(
//                    authorizationRequest.getUiLocales().stream()
//                            .filter(uiLocale -> serverConfiguration.getUiLocales().contains(uiLocale))
//                            .findFirst()
//                            .orElse(serverConfiguration.getUiLocales().get(0)));
//        }
    }

    @SuppressWarnings("unused")
    protected void validateResponseMode(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (hasText(authorizationRequest.getResponseMode())) {
            if (!serverConfiguration.getResponseModes().contains(authorizationRequest.getResponseMode())) {
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
    protected void validateIssuerState(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) {
        if (hasText(authorizationRequest.getIssuerState()) && !authorizationRequest.getIssuerState().matches("^[\\x20-\\x7E]+$")) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter issuer_state.", 400);
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
                if (!getConfiguration().supportsAuthorizationDetailsType(authorizationDetail.getType())) {
                    throw new OAuth2Exception(OAuth2Exception.INVALID_AUTHORIZATION_DETAILS, "Unsupported authorization_details type.", 400);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    protected void validateResource(ResourceIndicatorSupport request, ClientMetadata clientMetadata) {
        if (request.getResource() != null) {
            final URI uri;
            try {
                uri = new URI(request.getResource());
            } catch (URISyntaxException e) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_TARGET, "Invalid parameter resource.", 400);
            }
            if (!uri.isAbsolute()) {
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
        serverConfiguration.getAuditLogger().auditPushedAuthorizationRequest(authorizationRequest);
        PushedAuthorizationResponse pushedAuthorizationResponse = Objects.requireNonNull(createResponse(authorizationRequest));
        serverConfiguration.getAuditLogger().auditPushedAuthorizationResponse(pushedAuthorizationResponse);
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
     *
     * @param authorizationRequest client authz request
     * @return pushed authorization response with request_uri
     */
    protected final PushedAuthorizationResponse createPushedAuthorizationResponse(PushedAuthorizationRequest authorizationRequest) {
        String requestUri = createRequestUri();
        authorizationRequest.setLifetimeSeconds(serverConfiguration.getAuthorizationRequestLifetimeSeconds());
        serverConfiguration.getCache().putAuthorizationRequest(requestUri, authorizationRequest);
        return PushedAuthorizationResponse.builder().expiresIn(authorizationRequest.expiresInSeconds()).requestUri(requestUri).build();
    }

    protected String validateDPoPProofAndGetDPoPJtk(String dPoPHeader, String clientId, URI endpoint) {
        SignedJWT dPopProof;
        try {
            dPopProof = SignedJWT.parse(dPoPHeader);
        } catch (ParseException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_DPOP_PROOF, "Invalid request. Failed to parse DPop header", 400, e);
        }

        long maximumTimeSkewSeconds = serverConfiguration.getDPopTimeSkewSeconds();
        long maxAgeSeconds = serverConfiguration.getDPopLifetimeSeconds();
        DPoPTokenRequestVerifier verifier = new DPoPTokenRequestVerifier(serverConfiguration.getDPopSigningAlgValuesSupported(), endpoint, maximumTimeSkewSeconds, maxAgeSeconds, null);
        try {
            JWKThumbprintConfirmation verify = verifier.verify(new DPoPIssuer(clientId), dPopProof, (Nonce) null); // TODO add Nonce later
            return verify.getValue().toString();
        } catch (InvalidDPoPProofException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_DPOP_PROOF, "Invalid request. DPop Proof header invalid", 400, e);
        } catch (JOSEException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_DPOP_PROOF, "Invalid request. DPop Proof header invalid JWK", 400, e);
        }

    }

    @Override
    public ChallengeResponse process(ChallengeRequest challengeRequest) throws OAuth2Exception {
        ChallengeResponse challengeResponse = new ChallengeResponse(issueAttestationChallenge());
        serverConfiguration.getAuditLogger().auditChallengeResponse(challengeResponse);
        return challengeResponse;
    }

    @Override
    public ClientMetadata authenticateClient(AuthenticatedRequest authenticatedRequest) {
        if (!authenticatedRequest.isAuthenticatedRequest()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Missing client authentication.", 401);
        }
        if (authenticatedRequest.hasMoreThanOneClientAuthMethod()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Multiple client authentications.", 401);
        }
        final ClientAuthentication clientAuthentication;
        if (authenticatedRequest.isAttestationBased()) {
            clientAuthentication = authenticateClientByAttestation(authenticatedRequest);
        } else if (authenticatedRequest.isNone()) {
            clientAuthentication = authenticateClientByNone(authenticatedRequest);
        } else {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unknown client authentication method.", 401);
        }
        final ClientMetadata clientMetadata;
        if (authenticatedRequest instanceof PushedAuthorizationRequest) {
            clientMetadata = ClientMetadata.builder().clientId(clientAuthentication.getClientId()).redirectUri(((PushedAuthorizationRequest) authenticatedRequest).getRedirectUri()).build();
        } else {
            clientMetadata = ClientMetadata.builder().clientId(clientAuthentication.getClientId()).build();
        }
        if (hasText(authenticatedRequest.getClientId()) && !Objects.equals(authenticatedRequest.getClientId(), clientMetadata.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Client authentication does not match parameter client_id.", 401);
        }
        authenticatedRequest.clearAuthentication();
        authenticatedRequest.setAuthenticatedClientId(clientAuthentication.getClientId());
        serverConfiguration.getAuditLogger().auditClientAuthentication(clientAuthentication);
        return clientMetadata;
    }
    private void validateJWTAudience(SignedJWT signedJWT) {
        try {
            if (signedJWT.getJWTClaimsSet().getAudience() == null || signedJWT.getJWTClaimsSet().getAudience().isEmpty()) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Missing JWT audience.", 401);
            }
            if (signedJWT.getJWTClaimsSet().getAudience().size() != 1) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unique JWT audience required.", 401);
            }
            if (!signedJWT.getJWTClaimsSet().getAudience().contains(serverConfiguration.getIssuer().toString())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unknown JWT audience.", 401);
            }
        } catch (ParseException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Unparsable JWT audience.", 401, e);
        }
    }

    protected ClientAuthentication authenticateClientByNone(AuthenticatedRequest authenticatedRequest) {
        return ClientAuthentication.builder().clientId(authenticatedRequest.getClientId()).tokenEndpointAuthMethod("none").build();
    }

        protected ClientAuthentication authenticateClientByAttestation(AuthenticatedRequest authenticatedRequest) {
        try {
            final String requestedClientId = authenticatedRequest.getClientId();
            final SignedJWT clientAttestationJWT = SignedJWT.parse(authenticatedRequest.getClientAttestation());
            final SignedJWT clientAttestationPoPJWT = SignedJWT.parse(authenticatedRequest.getClientAttestationPoP());
            validateClientAttestation(clientAttestationJWT);
            validateClientAttestationPoP(clientAttestationJWT, clientAttestationPoPJWT);
            final String attestedClientId = clientAttestationJWT.getJWTClaimsSet().getSubject();
            if (!StringUtils.hasText(attestedClientId)) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Missing subject.", 401);
            }
            if (StringUtils.hasText(requestedClientId) && !requestedClientId.equals(attestedClientId)) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Invalid subject.", 401);
            }
            if (!Objects.equals(attestedClientId, clientAttestationPoPJWT.getJWTClaimsSet().getIssuer())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Invalid issuer.", 401);
            }
            return ClientAuthentication.builder()
                    .clientId(attestedClientId)
                    .tokenEndpointAuthMethod("attest_jwt_client_auth")
                    .clientAttestation(authenticatedRequest.getClientAttestation())
                    .clientAttestationPoP(authenticatedRequest.getClientAttestationPoP())
                    .attestationChallenge(clientAttestationPoPJWT.getJWTClaimsSet().getStringClaim("challenge"))
                    .walletName(clientAttestationJWT.getJWTClaimsSet().getStringClaim("wallet_name"))
                    .walletLink(clientAttestationJWT.getJWTClaimsSet().getStringClaim("wallet_link"))
                    .build();
        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Attestation processing failed.", 401, e);
        }
    }

    protected void validateClientAttestation(SignedJWT clientAttestation) {
        if (!serverConfiguration.getClientAttestationSigningAlgValuesSupported().contains(clientAttestation.getHeader().getAlgorithm())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. Unsupported JWT signing algorithm.", 401);
        }
        ECKey clientAttesterPublicKey = extractJWKFromX509Certificate(extractX509CertificateFromJWTHeader(clientAttestation));
        verifyJWTSignature(clientAttestation, clientAttesterPublicKey);
        verifyJWTType(clientAttestation, "oauth-client-attestation+jwt");
        validateJWTLifetime(clientAttestation);
    }

    void validateAttestationChallenge(SignedJWT clientAttestationPoP) {
        String challenge = null;
        try {
            challenge = clientAttestationPoP.getJWTClaimsSet().getStringClaim("challenge");
            if (! StringUtils.hasText(challenge)) {
                throw new UseAttestationChallengeOAuth2Exception("Invalid client authentication. Missing challenge in attestation pop.", issueAttestationChallenge());
            }
            Challenge serverIssuedChallenge = serverConfiguration.getCache().getChallenge(challenge);
            if (serverIssuedChallenge == null) {
                throw new UseAttestationChallengeOAuth2Exception("Invalid client authentication. Unknown challenge in attestation pop.", issueAttestationChallenge());
            }
            if (! serverIssuedChallenge.isValidNow()) {
                throw new UseAttestationChallengeOAuth2Exception("Invalid client authentication. Expired challenge in attestation pop.", issueAttestationChallenge());
            }
        } catch (ParseException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. Failed to parse attestation pop.", 401);
        } finally {
            if (challenge != null) {
                serverConfiguration.getCache().removeChallenge(challenge);
            }
        }
    }

    protected void validateClientAttestationPoP(SignedJWT clientAttestation, SignedJWT clientAttestationPoP) {
        if (!serverConfiguration.getClientAttestationPoPSigningAlgValuesSupported().contains(clientAttestationPoP.getHeader().getAlgorithm())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. Unsupported PoP JWT signing algorithm.", 401);
        }
        ECKey clientKey = extractJWKFromCnf(clientAttestation);
        verifyJWTSignature(clientAttestationPoP, clientKey);
        verifyJWTType(clientAttestationPoP, "oauth-client-attestation-pop+jwt");
        validateJWTAudience(clientAttestationPoP);
        validateAttestationChallenge(clientAttestationPoP);
    }

    void verifyJWTType(SignedJWT signedJWT, String type) {
        if (signedJWT.getHeader().getType() == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. Missing JWT typ header.", 401);
        }
        if (!type.equals(signedJWT.getHeader().getType().getType())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. Invalid JWT typ header.", 401);
        }
    }

    void verifyJWTSignature(SignedJWT signedJWT, ECKey publicKey) {
        try {
            JWSVerifier jwsVerifier = new ECDSAVerifier(publicKey);
            if (!signedJWT.verify(jwsVerifier)) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Invalid JWT signature.", 401);
            }
        } catch (JOSEException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Failed to verify JWT signature.", 401, e);
        }
    }

    void validateJWTLifetime(final SignedJWT signedJWT) {
        try {
            if (signedJWT.getJWTClaimsSet().getExpirationTime() == null) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. Missing JWT exp claim.", 401);
            }
            if (signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. JWT expired.", 401);
            }
        } catch (ParseException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT, "Invalid client authentication. JWT unparsable.", 401, e);
        }
    }

    X509Certificate extractX509CertificateFromJWTHeader(SignedJWT signedJWT) {
        List<com.nimbusds.jose.util.Base64> x5c = signedJWT.getHeader().getX509CertChain();
        if (x5c == null || x5c.isEmpty()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. JWT missing x5c header.", 401);
        }
        try {
            List<X509Certificate> x5cList = X509CertChainUtils.parse(x5c);
            if (x5cList.isEmpty()) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. JWT missing x5c header.", 401);
            }
            return x5cList.getFirst();
        } catch (ParseException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. JWT invalid x5c header.", 401, e);
        }
    }

    ECKey extractJWKFromJWTHeader(JWK jwk) {
        if (!KeyType.EC.equals(jwk.getKeyType()) || !(jwk instanceof ECKey ecKey)) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. JWT invalid jwk header.", 401);
        }
        if (ecKey.isPrivate()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. Client passed private key in jwt header.", 401);
        }
        return ecKey;
    }

    ECKey extractJWKFromX509Certificate(X509Certificate x509Certificate) {
        try {
            return JWK.parse(x509Certificate).toECKey();
        } catch (JOSEException e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. JWT invalid x5c header.", 401);
        }
    }

    ECKey extractJWKFromCnf(SignedJWT signedJWT) {
        try {
            ECKey ecKey = ECKey.parse((Map) signedJWT.getJWTClaimsSet().getJSONObjectClaim("cnf").get("jwk"));
            if (ecKey.isPrivate()) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. Client passed private key in cnf claim.", 401);
            }
            return ecKey;
        } catch (Exception e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_CLIENT_ATTESTATION, "Invalid client authentication. JWT invalid or missing cnf claim.", 401, e);
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
        if (!authorizationRequest.getRequestUri().matches("^urn:%s:.*$".formatted(serverConfiguration.getInternalId()))) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter request_uri.", 400);
        }
    }

    protected void validateClientId(AuthorizationRequest authorizationRequest) {
        if (serverConfiguration.isDisableClientIdCheckOnPARAuthorizationRequests() && !hasText(authorizationRequest.getClientId())) {
            return;
        }
        if (!hasText(authorizationRequest.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter client_id.", 400);
        }
    }

    protected String createRequestUri() {
        return "urn:%s:%s".formatted(serverConfiguration.getInternalId(), generateId());
    }

    @Override
    public PushedAuthorizationRequest process(AuthorizationRequest authorizationRequest) {
        validate(authorizationRequest);
        final PushedAuthorizationRequest pushedAuthorizationRequest = serverConfiguration.getCache().getAuthorizationRequest(authorizationRequest.getRequestUri());
        if (pushedAuthorizationRequest == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter request_uri. request_uri does not exist.", 400);
        }
        if (!pushedAuthorizationRequest.isValidNow()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter request_uri. request_uri has expired.", 400);
        }
        if (hasText(authorizationRequest.getClientId()) && !authorizationRequest.getClientId().equals(pushedAuthorizationRequest.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter client_id. The request was pushed by another client.", 400);
        }
        // do only support PAR, no need to check incoming DPoP

        serverConfiguration.getCache().removeAuthorizationRequest(authorizationRequest.getRequestUri());
        serverConfiguration.getAuditLogger().auditAuthorizationRequest(authorizationRequest);
        return pushedAuthorizationRequest;
    }

    protected String calcAudience(PushedAuthorizationRequest request) {
        if (request.hasResourceIndicator()) {
            return request.getResource();
        }
        if (serverConfiguration.getAccessTokenDefaultAudience() != null) {
            return serverConfiguration.getAccessTokenDefaultAudience().toString();
        }
        return null;
    }

    @Override
    public AuthorizationResponse authorize(PushedAuthorizationRequest pushedAuthorizationRequest, Authorization authorization) {
        String code = generateId();
        authorization.setNonce(pushedAuthorizationRequest.getNonce());
        authorization.setCodeChallenge(pushedAuthorizationRequest.getCodeChallenge());
        authorization.setLifetimeSeconds(serverConfiguration.getAuthorizationLifetimeSeconds());
        authorization.setClientId(pushedAuthorizationRequest.getClientId());
        authorization.setAud(calcAudience(pushedAuthorizationRequest));
        authorization.setScope(String.join(" ", pushedAuthorizationRequest.getScope()));
        authorization.setIssuerState(pushedAuthorizationRequest.getIssuerState());
        if (!hasText(authorization.getAcr())) {
            authorization.setAcr(pushedAuthorizationRequest.getResolvedAcrValue());
        }

        authorization.setDpopJkt(pushedAuthorizationRequest.getResolvedDpopJkt()); // parse og sjekk også dpop_jkt
        validateAuthorization(authorization);

        serverConfiguration.getCache().putAuthorization(code, authorization);
        serverConfiguration.getAuditLogger().auditAuthorization(authorization);
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .redirectUri(pushedAuthorizationRequest.getRedirectUri())
                .aud(pushedAuthorizationRequest.getClientId())
                .iss(serverConfiguration.isAuthorizationResponseIssParameterSupported() ? serverConfiguration.getIssuer().toString() : null)
                .responseMode(pushedAuthorizationRequest.getResolvedResponseMode())
                .code(code)
                .state(pushedAuthorizationRequest.getState())
                .build();
        serverConfiguration.getAuditLogger().auditAuthorizationResponse(authorizationResponse);

        return authorizationResponse;
    }

    @Override
    public AuthorizationResponse errorResponse(PushedAuthorizationRequest pushedAuthorizationRequest, String error, String errorDescription) {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .redirectUri(pushedAuthorizationRequest.getRedirectUri())
                .aud(pushedAuthorizationRequest.getClientId())
                .iss(serverConfiguration.isAuthorizationResponseIssParameterSupported() ? serverConfiguration.getIssuer().toString() : null)
                .responseMode(pushedAuthorizationRequest.getResolvedResponseMode())
                .error(error)
                .errorDescription(errorDescription)
                .state(pushedAuthorizationRequest.getState())
                .build();
        serverConfiguration.getAuditLogger().auditAuthorizationResponse(authorizationResponse);
        return authorizationResponse;
    }

    @Override
    public AuthorizationResponse panicErrorResponse(ClientMetadata clientMetadata, String error, String errorDescription) {
        Objects.requireNonNull(clientMetadata);
        Objects.requireNonNull(error);
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .redirectUri(clientMetadata.getRedirectUris().getFirst())
                .aud(clientMetadata.getClientId())
                .iss(serverConfiguration.isAuthorizationResponseIssParameterSupported() ? serverConfiguration.getIssuer().toString() : null)
                .responseMode("query.jwt")
                .error(error)
                .errorDescription(errorDescription)
                .build();
        serverConfiguration.getAuditLogger().auditAuthorizationResponse(authorizationResponse);
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
        serverConfiguration.getAuditLogger().auditTokenRequest(tokenRequest);
        Authorization authorization = serverConfiguration.getCache().getAuthorization(tokenRequest.getCode());
        if (authorization == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant does not exist.", 400);
        }
        if (!authorization.isValidNow()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant has expired.", 400);
        }
        if (!Objects.equals(clientMetadata.getClientId(), authorization.getClientId())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. The grant is not issued to authenticated client.", 400);
        }
        if ((getConfiguration().isRequirePkce() || hasText(authorization.getCodeChallenge())) && !validateCodeVerifier(tokenRequest.getCodeVerifier(), authorization.getCodeChallenge())) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_GRANT, "Invalid grant. Invalid code_verifier.", 400);
        }
        if (tokenRequest.getDPoPHeader() != null) {
            String dpopJtk = validateDPoPProofAndGetDPoPJtk(tokenRequest.getDPoPHeader(), tokenRequest.getClientId(), serverConfiguration.getTokenEndpoint());
            if (hasText(authorization.getDpopJkt()) && !Objects.equals(dpopJtk, authorization.getDpopJkt())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_DPOP_PROOF, "Invalid DPop. The DPop header is invalid.", 400);
            }
            authorization.setDpopJkt(dpopJtk);
        }

        serverConfiguration.getCache().removeAuthorization(tokenRequest.getCode());
        if (tokenRequest.hasResourceIndicator()) {
            authorization.setAud(tokenRequest.getResource());
        }
        try {
            TokenResponse tokenResponse = createTokenResponse(authorization);
            if (serverConfiguration.getUserinfoEndpoint() != null) {
                authorization = authorization.toBuilder().build();
                authorization.setLifetimeSeconds(serverConfiguration.getAccessTokenLifetimeSeconds());
                serverConfiguration.getCache().putAccessTokenAndAuthorization(tokenResponse.getAccessToken(), authorization);
            }
            serverConfiguration.getAuditLogger().auditTokenResponse(tokenResponse);
            return tokenResponse;
        } catch (Exception e) {
            throw new OAuth2Exception("internal_error", "The server failed to process the request", 500, e);
        }
    }

    protected void validateAuthorization(Authorization authorization) {
        Objects.requireNonNull(authorization);
        Objects.requireNonNull(authorization.getAud(), "authorization must have an audience");
        Objects.requireNonNull(authorization.getSub(), "authorization must have a sub");
    }

    protected final TokenResponse createTokenResponse(Authorization authorization) throws JOSEException {
        validateAuthorization(authorization);
        boolean isOpenIDConnect = authorization.getScope().contains("openid");
        TokenResponse.TokenResponseBuilder builder = TokenResponse.builder()
                .idToken(isOpenIDConnect ? createIDToken(authorization) : null)
                .accessToken(createAccessToken(authorization))
                .expiresInSeconds(serverConfiguration.getAccessTokenLifetimeSeconds());
        if (hasText(authorization.getDpopJkt())) {
            builder.tokenType("DPoP");
        }
        return builder.build();
    }

    private String createIDToken(Authorization authorization) throws JOSEException {
        String idToken;
        JWTClaimsSet.Builder idTokenClaimsSetBuilder = new JWTClaimsSet.Builder()
                .jwtID(generateId())
                .issuer(serverConfiguration.getIssuer().toString())
                .audience(authorization.getClientId())
                .expirationTime(new Date(new Date().getTime() + (serverConfiguration.getIdTokenLifetimeSeconds() * 1000L)))
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
                .issuer(serverConfiguration.getIssuer().toString())
                .audience(authorization.getAud())
                .claim("client_id", authorization.getClientId())
                .claim("scope", authorization.getScope())
                .claim("issuer_state", authorization.getIssuerState())
                .expirationTime(new Date(new Date().getTime() + (serverConfiguration.getAccessTokenLifetimeSeconds() * 1000L)))
                .issueTime(new Date())
                .subject(authorization.getSub());
        if (hasText(authorization.getDpopJkt())) {
            Map<String, Object> jkt = JsonObjectBuilder.builder().addAttribute("jkt", authorization.getDpopJkt()).build();
            accessTokenClaimsSetBuilder.claim("cnf", jkt);
        }
        authorization.getAttributes().forEach(accessTokenClaimsSetBuilder::claim);
        accessToken = signJwt("at+JWT", accessTokenClaimsSetBuilder.build());
        return accessToken;
    }


    @Override
    public UserInfoResponse process(UserInfoRequest userInfoRequest) {
        validate(userInfoRequest);
        serverConfiguration.getAuditLogger().auditUserInfoRequest(userInfoRequest);
        Authorization authorization = serverConfiguration.getCache().getAuthorizationByAccessToken(userInfoRequest.getBearerToken());
        if (authorization == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_TOKEN, "Invalid token. The token does not exist.", 401);
        }
        if (!authorization.isValidNow()) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_TOKEN, "Invalid token. The token has expired.", 401);
        }
        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                .sub(authorization.getSub())
                .build();
        serverConfiguration.getAuditLogger().auditUserInfoResponse(userInfoResponse);
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
                        .issuer(serverConfiguration.getIssuer().toString())
                        .expirationTime(new Date(new Date().getTime() + (serverConfiguration.getAuthorizationLifetimeSeconds() * 1000L)))
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
        if (!hasText(tokenRequest.getGrantType()) || !serverConfiguration.getGrantTypesSupported().contains(tokenRequest.getGrantType())) {
            throw new OAuth2Exception(OAuth2Exception.UNSUPPORTED_GRANT_TYPE, "Invalid parameter grant_type. Supported: " + serverConfiguration.getGrantTypesSupported(), 400);
        }
        if (!hasText(tokenRequest.getRedirectUri()) && clientMetadata.getRedirectUris().size() > 1) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter redirect_uri.", 400);
        }
        if (hasText(tokenRequest.getCodeVerifier()) && !tokenRequest.getCodeVerifier().matches("^[A-Za-z0-9\\-._~]{43,128}$")) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter code_verifier.", 400);
        }
        validateResource(tokenRequest, clientMetadata);
        // code flow specific
        if ("authorization_code".equals(tokenRequest.getGrantType())) {
            if (!hasText(tokenRequest.getCode())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter code.", 400);
            }
            if (!hasText(tokenRequest.getCodeVerifier()) && getConfiguration().isRequirePkce()) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing parameter code_verifier. PKCE is required.", 400);
            }
            if (hasText(tokenRequest.getPreAuthorizedCode())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Illegal parameter pre-authorized_code.", 400);
            }
            if (hasText(tokenRequest.getTxCode())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Illegal parameter tx_code.", 400);
            }
        }
        // pre-authorized code flow specific
        else if ("urn:ietf:params:oauth:grant-type:pre-authorized_code".equals(tokenRequest.getGrantType())) {
            if (!hasText(tokenRequest.getPreAuthorizedCode())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid parameter pre-authorized_code.", 400);
            }
            if (hasText(tokenRequest.getCode())) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Illegal parameter code.", 400);
            }
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
        return serverConfiguration.findClient(clientId);
    }

    @Override
    public OAuth2ServerConfiguration getConfiguration() {
        return serverConfiguration;
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

    private String issueAttestationChallenge() {
        Challenge challenge = new Challenge(generateId(), serverConfiguration.getChallengeLifetimeSeconds());
        serverConfiguration.getCache().putChallenge(challenge);
        return challenge.challenge();
    }

    protected String signJwt(String type, JWTClaimsSet jwtClaimsSet) throws JOSEException {
        JWK jwk = serverConfiguration.getJwk();
        boolean isEC = KeyType.EC.equals(jwk.getKeyType());
        JWSSigner signer = isEC ? new ECDSASigner(jwk.toECKey()) : new RSASSASigner(jwk.toRSAKey());
        JWSAlgorithm signingAlgorithm = isEC ? JWSAlgorithm.ES256 : JWSAlgorithm.RS256;
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader
                        .Builder(signingAlgorithm)
                        .type(StringUtils.hasText(type) ? new JOSEObjectType(type) : null)
                        .keyID(serverConfiguration.getJwk().getKeyID())
                        .build(),
                jwtClaimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

}
