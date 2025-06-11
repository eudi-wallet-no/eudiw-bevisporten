package no.idporten.sdk.oidcserver.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import no.idporten.sdk.oidcserver.audit.NullAuditLogger;
import no.idporten.sdk.oidcserver.audit.OpenIDConnectAuditLogger;
import no.idporten.sdk.oidcserver.cache.OpenIDConnectCache;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.util.URIUtils;

import java.net.URI;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

/**
 * Configuration settings for an oidc server implemented with the SDK.  Some sanity is checked on server startup.  The
 * application using the SDK must know the OAuth2/OpenID Connect endpoints it exposes and configure the SDK with correct
 * values.
 */
@Getter
@Builder
public final class OpenIDConnectSdkConfiguration {

    /**
     * OAuth2 issuer uri.  The base uri for this server.  The discovery endpoint must be served relative to this uri.
     *
     * @param issuer servers issuer uri
     */
    private URI issuer;

    /**
     * Uri to endpoint for pushed authorization requests to this server.
     *
     * @param pushedAuthorizationRequestEndpoint uri
     */
    private URI pushedAuthorizationRequestEndpoint;

    /**
     * Uri to endpoint for authorization requests to this server.
     *
     * @param authorizationEndpoint uri
     */
    private URI authorizationEndpoint;

    /**
     * Uri to endpoint for token requests to this server.
     *
     * @param tokenEndpoint uri
     */
    private URI tokenEndpoint;

    /**
     * List of supported algorithms clients can use for signing authenticated requests.
     */
    @Builder.Default
    private List<JWSAlgorithm> tokenEndpointAuthSigningAlgValuesSupported= List.of(JWSAlgorithm.HS256);

    /**
     * Uri to endpoint for json web keys used by this server.
     *
     * @param jwksUri uri
     */
    private URI jwksUri;

    /**
     * Uri to endpoint for userinfo requests to this server.  It is optional to implement this endpoint.
     *
     * @param userinfoEndpoint uri
     */
    private URI userinfoEndpoint;

    /**
     * Internal id used in generated keys for OAuth2 par request_uri.
     *
     * @param internalId id
     */
    @Builder.Default
    private String internalId = "idporten";

    /**
     * The set of scopes supported by this OAuth2 server.
     *
     * @param scopesSupported scopes
     */
    @Singular("scopeSupported")
    private Set<String> scopesSupported;

    /**
     * The set of claims supported by this OAuth2 server.
     *
     * @param claimsSupported claims this server may include in id_token
     */
    @Singular("claimSupported")
    private Set<String> claimsSupported;

    /**
     * Advertise support for rich authorization requests in server discovery metadata.
     *
     * @param authorizationDetailsTypesSupported support for rich authorization requests?
     */
    @Singular("authorizationDetailsTypeSupported")
    private Set<String> authorizationDetailsTypesSupported;

    /**
     * The set of acr values supported by this OpenID Connect server.
     *
     * @param acrValues acr values this server may include in id_token
     */
    @Singular("acrValue")
    private List<String> acrValues;

    /**
     * The ui locales supported by this OpenID connect server.
     *
     * @param uiLocales supported ui locales
     */
    @Singular("uiLocale")
    private List<String> uiLocales;

    /**
     * Response modes supported by this OAuth2 server.
     *
     * @params responseModes response modes supported by the server
     */
    @Singular("responseMode")
    private Set<String> responseModes;

    /**
     * PKCE code challenge methods supported by server
     *
     * @param codeChallengesMethodSupported code challenge methods supported by this OAuth2 server
     */
    @Singular("codeChallengeMethodSupported")
    private Set<String> codeChallengeMethodsSupported;


    /**
     * Lifetime in seconds for pushed authorization requests to this server.  This is the processing time for the client
     * to redirect to the authorization endpoint.
     *
     * @param authorizationRequestLifetimeSeconds lifetime in seconds
     */
    @Builder.Default
    private int authorizationRequestLifetimeSeconds = 60;

    /**
     * Lifetime in seconds for the authorizations added to this server.  This is the lifetime of the authorization code
     * and the processing time for the client to retrieve tokens.
     *
     * @param authorizationLifetimeSeconds lifetime in seconds
     */
    @Builder.Default
    private int authorizationLifetimeSeconds = 60;

    /**
     * Lifetime in seconds for id_token issued by this server.  This is the processing time for the client to check
     * the id_token's validity.
     *
     * @param idTokenLifetimeSeconds lifetime in seconds
     */
    @Builder.Default
    private int idTokenLifetimeSeconds = 120;

    /**
     * Lifetime in seconds for access_token issued by this server.  This is the usage time for clients to use the
     * access_token towards the OAuth2-protected endpoints exposed by this server.
     *
     * @param accessTokenLifetimeSeconds lifetime in seconds
     */
    @Builder.Default
    private int accessTokenLifetimeSeconds = 120;

    /**
     * The set of client's this OAuth2 server will accept requests from.
     *
     * @param clients OAuth2 clients
     */
    @Singular("client")
    private Set<ClientMetadata> clients;

    /**
     * Cache implementation for OAuth2 objects that need to be stored between client requests.
     *
     * @param cache cache implementation
     */
    private OpenIDConnectCache cache;

    /**
     * Audit implementation for this server.  See interface definition for hooks.
     *
     * @param auditLogger audit logger implementation
     */
    @Builder.Default
    private OpenIDConnectAuditLogger auditLogger = new NullAuditLogger();

    /**
     * Private key used to sign tokens.
     *
     * @param jwk private key
     */
    private RSAKey jwk;

    /**
     * Algorithm for jws signing (tokens, responses).
     */
    @Builder.Default
    private JWSAlgorithm defaultSigningAlgorithm = JWSAlgorithm.RS256;

    /**
     * Require pushed authorization requests from clients.
     *
     * @param requirePushedAuthorizationRequests require pushed authorization requests?
     */
    @Builder.Default
    private boolean requirePushedAuthorizationRequests = true;

    /**
     * Require PKCE.
     */
    @Builder.Default
    private boolean requirePkce = true;

    /**
     * Support iss parameter on authorization responses.  Default is on.
     */
    @Builder.Default
    private boolean authorizationResponseIssParameterSupported = true;

    /**
     * Backward compatibility - avoid in new applications - ignore client_id parameter missing on pushed authorization requests
     *
     * @param disableClientIdCheckOnPARAuthorizationRequests require pushed authorization requests?
     */
    private boolean disableClientIdCheckOnPARAuthorizationRequests;

    public static OpenIDConnectSdkConfigurationBuilder builder() {
        return new OpenIDConnectSdkConfigurationBuilder()
                .responseMode("query")
                .scopeSupported("openid")
                .codeChallengeMethodSupported("S256");
    }

    public static class OpenIDConnectSdkConfigurationBuilder {

        private RSAKey jwk;

        public OpenIDConnectSdkConfigurationBuilder keystore(KeyStore keyStore, String keyAlias, String keyPassword) {
            try {
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
                Certificate certificate = keyStore.getCertificate(keyAlias);
                PublicKey publicKey = certificate.getPublicKey();
                this.jwk = new RSAKey.Builder((RSAPublicKey) publicKey)
                        .privateKey(privateKey)
                        .keyUse(KeyUse.SIGNATURE)
                        .keyIDFromThumbprint()
                        .build();
                return this;
            } catch (Exception e) {
                throw new IllegalArgumentException("The SDK configuration failed to convert keystore into JWK", e);
            }
        }

        public OpenIDConnectSdkConfigurationBuilder jwk(RSAKey jwk) {
            this.jwk = jwk;
            return this;
        }

    }

    /**
     * Find a client by client_id.
     *
     * @param clientId
     * @return client or null if not found
     */
    public final ClientMetadata findClient(String clientId) {
        return clients.stream()
                .filter(clientMetadata -> Objects.equals(clientId, clientMetadata.getClientId()))
                .findFirst()
                .orElse(null);
    }

    public void validate() {
        // TODO
        validateUri("issuer", true, issuer);
        validateUri("pushedAuthorizationRequestEndpoint", false, pushedAuthorizationRequestEndpoint);
        validateUri("authorizationEndpoint", false, authorizationEndpoint);
        validateUri("tokenEndpoint", false, tokenEndpoint);
        validateUri("userinfoEndpoint", false, userinfoEndpoint);
        validateUri("jwksUri", false, jwksUri);
        validateLifetime("authorizationRequestLifetimeSeconds", authorizationRequestLifetimeSeconds);
        validateLifetime("authorizationLifetimeSeconds", authorizationLifetimeSeconds);
        validateLifetime("idTokenLifetimeSeconds", idTokenLifetimeSeconds);
        validateClients();
//        validateList("acrValues", true, acrValues);
//        validateList("uiLocales", true, uiLocales);
        validateList("responseModes", true, responseModes, "query", "form_post", "query.jwt");
        validateScopes();
//        validateList("claimsSupported", false, claimsSupported);
        validateCodeChallengeMethods();
        Objects.requireNonNull(jwk);
        Objects.requireNonNull(defaultSigningAlgorithm);
        Objects.requireNonNull(tokenEndpointAuthSigningAlgValuesSupported);
    }

    protected final void validateRequiredProperty(String property, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The SDK requires a value for %s.".formatted(property));
        }
        if (value instanceof String && ((String) value).isEmpty()) {
            throw new IllegalArgumentException("The SDK requires a value for %s.".formatted(property));
        }
    }

    protected final void validateUri(String property, boolean required, URI uri)  {
        if (required) {
            validateRequiredProperty(property, uri);
        }
        if (uri == null) {
            return;
        }
        if (! ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("The SDK requires a http(s) uri for %s.".formatted(property));
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("The SDK requires an uri without fragment for %s.".formatted(property));
        }
    }

    protected final void validateLifetime(String property, int lifetime) {
        if (lifetime <= 0) {
            throw new IllegalArgumentException("The SDK requires a positive lifetime for %s.".formatted(property));
        }
    }

    protected final void validateClients() {
        if (clients == null || clients.isEmpty()) {
// TODO            throw new IllegalArgumentException("The SDK requires a list of client metadata");
        }
        Set<String> clientIds = new HashSet<>();
        for (ClientMetadata clientMetadata : clients) {
            clientMetadata.validate();
            if (clientIds.contains(clientMetadata.getClientId())) {
                throw new IllegalArgumentException("Client %s has already been registered with the SDK.".formatted(clientMetadata.getClientId()));
            }
            clientIds.add(clientMetadata.getClientId());
        }
    }

    protected final void validateScopes() {
        if (scopesSupported == null || scopesSupported.isEmpty()) {
            throw new IllegalArgumentException("The SDK requires a list of supported scopes");
        }
        if (! scopesSupported.contains("openid")) {
            throw new IllegalArgumentException("The SDK requires that the openid scope is supported");
        }
    }

    protected final void validateCodeChallengeMethods()  {
        if (requirePkce && codeChallengeMethodsSupported.isEmpty()) {
            throw new IllegalArgumentException("The SDK requires a list of supported code challenge methods when PKCE is required");
        }
        if (!codeChallengeMethodsSupported.isEmpty() && codeChallengeMethodsSupported.retainAll(Collections.singleton("S256"))) {
            throw new IllegalArgumentException("The SDK only supported code challenge method S256");
        }
    }

    protected final void validateList(String property, boolean required, Collection<String> list, String... acceptedValues) {
        if (required && (list == null || list.isEmpty())) {
            throw new IllegalArgumentException("The SDK requires a list of values for %s.".formatted(property));
        }
        if (list == null) {
            return;
        }
        if (! list.stream().allMatch(s -> s != null && ! s.isEmpty())) {
            throw new IllegalArgumentException("The SDK requires a list of non-empty values for %s.".formatted(property));
        }
        if (acceptedValues.length > 0) {
            List<String> accepted = List.of(acceptedValues);
            if (! list.stream().allMatch(s -> accepted.contains(s))) {
                throw new IllegalArgumentException("The SDK detected illegal values in list of values for %s.".formatted(property));
            }
        }
    }

    public boolean supportsScope(String scope) {
        return getScopesSupported().contains(scope);
    }

    public boolean supportsAuthorizationDetailsType(String type) {
        return getAuthorizationDetailsTypesSupported().contains(type);
    }

    public boolean supportsClaim(String claim) {
        return getClaimsSupported().contains(claim);
    }

    /**
     * Gets calculated endpoint uri for OpenID Connect discovery endpoint.
     */
    public URI getOidcDiscoveryEndpoint() {
        return URIUtils.appendPath(getIssuer(), "/.well-known/openid-configuration");
    }

}
