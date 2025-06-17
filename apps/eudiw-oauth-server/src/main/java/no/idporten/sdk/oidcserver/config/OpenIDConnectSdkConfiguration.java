package no.idporten.sdk.oidcserver.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
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
import java.security.interfaces.ECPublicKey;
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
     */
    private URI issuer;

    /**
     * The set of response types supported by this OAuth2 server.
     * Default is "code" OAuth2.
     */
    @Builder.Default
    private List<String> grantTypesSupported = List.of("authorization_code");

    /**
     * Uri to endpoint for pushed authorization requests to this server.
     */
    private URI pushedAuthorizationRequestEndpoint;

    /**
     * Uri to endpoint for authorization requests to this server.
     */
    private URI authorizationEndpoint;

    /**
     * Uri to endpoint for token requests to this server.
     */
    private URI tokenEndpoint;

    /**
     * List of supported algorithms clients can use for signing authenticated requests.
     */
    @Builder.Default
    private List<JWSAlgorithm> tokenEndpointAuthSigningAlgValuesSupported= List.of(JWSAlgorithm.HS256);

    /**
     * Uri to endpoint for json web keys used by this server.
     */
    private URI jwksUri;

    /**
     * Uri to endpoint for userinfo requests to this server.  It is optional to implement this endpoint.
     */
    private URI userinfoEndpoint;

    /**
     * Internal id used in generated keys for OAuth2 par request_uri.
     */
    @Builder.Default
    private String internalId = "idporten";

    /**
     * The set of scopes supported by this OAuth2 server.
     */
    @Singular("scopeSupported")
    private Set<String> scopesSupported;

    /**
     * The set of claims supported by this OAuth2 server.
     */
    @Singular("claimSupported")
    private Set<String> claimsSupported;

    /**
     * Advertise support for rich authorization requests in server discovery metadata.
     */
    @Singular("authorizationDetailsTypeSupported")
    private Set<String> authorizationDetailsTypesSupported;

    /**
     * The set of acr values supported by this OpenID Connect server. ACR values this server may include in id_token.
     */
    @Singular("acrValue")
    private List<String> acrValues;

    /**
     * The ui locales supported by this OpenID connect server.
     */
    @Singular("uiLocale")
    private List<String> uiLocales;

    /**
     * Response modes supported by this OAuth2 server.
     */
    @Singular("responseMode")
    private Set<String> responseModes;

    /**
     * PKCE code challenge methods supported by server
     */
    @Singular("codeChallengeMethodSupported")
    private Set<String> codeChallengeMethodsSupported;


    /**
     * Lifetime in seconds for pushed authorization requests to this server.  This is the processing time for the client
     * to redirect to the authorization endpoint.
     */
    @Builder.Default
    private int authorizationRequestLifetimeSeconds = 60;

    /**
     * Lifetime in seconds for the authorizations added to this server.  This is the lifetime of the authorization code
     * and the processing time for the client to retrieve tokens.
     */
    @Builder.Default
    private int authorizationLifetimeSeconds = 60;

    /**
     * Lifetime in seconds for id_token issued by this server.  This is the processing time for the client to check
     * the id_token's validity.
     */
    @Builder.Default
    private int idTokenLifetimeSeconds = 120;

    /**
     * Lifetime in seconds for access_token issued by this server.  This is the usage time for clients to use the
     * access_token towards the OAuth2-protected endpoints exposed by this server.
     */
    @Builder.Default
    private int accessTokenLifetimeSeconds = 120;

    /**
     * The set of clients this OAuth2 server will accept requests from.
     */
    @Singular("client")
    private Set<ClientMetadata> clients;

    /**
     * Cache implementation for OAuth2 objects that need to be stored between client requests.
     */
    private OpenIDConnectCache cache;

    /**
     * Audit implementation for this server.  See interface definition for hooks.
     */
    @Builder.Default
    private OpenIDConnectAuditLogger auditLogger = new NullAuditLogger();

    /**
     * Private key used to sign tokens.
     */
    private JWK jwk;

    /**
     * Algorithm for jws signing (tokens, responses).
     */
    @Builder.Default
    private JWSAlgorithm defaultSigningAlgorithm = JWSAlgorithm.RS256;

    /**
     * Require pushed authorization requests from clients.
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
     * TODO: remove in future versions
     */
    private boolean disableClientIdCheckOnPARAuthorizationRequests;

    public static OpenIDConnectSdkConfigurationBuilder builder() {
        return new OpenIDConnectSdkConfigurationBuilder()
                .responseMode("query")
                .scopeSupported("openid")
                .codeChallengeMethodSupported("S256");
    }

    public static class OpenIDConnectSdkConfigurationBuilder {

        private JWK jwk;

        public OpenIDConnectSdkConfigurationBuilder keystore(KeyStore keyStore, String keyAlias, String keyPassword) {
            try {
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
                Certificate certificate = keyStore.getCertificate(keyAlias);
                PublicKey publicKey = certificate.getPublicKey();
                if (publicKey instanceof ECPublicKey) {
                    this.jwk = new ECKey.Builder(Curve.forECParameterSpec(((ECPublicKey) publicKey).getParams()), (ECPublicKey) publicKey)
                            .privateKey(privateKey)
                            .keyUse(KeyUse.SIGNATURE)
                            .keyIDFromThumbprint()
                            .build();

                } else {
                    this.jwk = new RSAKey.Builder((RSAPublicKey) publicKey)
                            .privateKey(privateKey)
                            .keyUse(KeyUse.SIGNATURE)
                            .keyIDFromThumbprint()
                            .build();
                }
                return this;
            } catch (Exception e) {
                throw new IllegalArgumentException("The SDK configuration failed to convert keystore into JWK", e);
            }
        }

        public OpenIDConnectSdkConfigurationBuilder jwk(JWK jwk) {
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
    public ClientMetadata findClient(String clientId) {
        return clients.stream()
                .filter(clientMetadata -> Objects.equals(clientId, clientMetadata.getClientId()))
                .findFirst()
                .orElse(null);
    }

    public void validate() {
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
        validateList("responseModes", true, responseModes, "query", "form_post", "query.jwt");
        validateScopes();
        validateCodeChallengeMethods();
        Objects.requireNonNull(jwk);
        Objects.requireNonNull(defaultSigningAlgorithm);
        Objects.requireNonNull(tokenEndpointAuthSigningAlgValuesSupported);
    }

    protected void validateRequiredProperty(String property, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The SDK requires a value for %s.".formatted(property));
        }
        if (value instanceof String && ((String) value).isEmpty()) {
            throw new IllegalArgumentException("The SDK requires a value for %s.".formatted(property));
        }
    }

    protected void validateUri(String property, boolean required, URI uri)  {
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

    protected void validateLifetime(String property, int lifetime) {
        if (lifetime <= 0) {
            throw new IllegalArgumentException("The SDK requires a positive lifetime for %s.".formatted(property));
        }
    }

    protected void validateClients() {
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

    protected void validateScopes() {
        if (scopesSupported == null || scopesSupported.isEmpty()) {
            throw new IllegalArgumentException("The SDK requires a list of supported scopes");
        }

    }

    protected void validateCodeChallengeMethods()  {
        if (requirePkce && codeChallengeMethodsSupported.isEmpty()) {
            throw new IllegalArgumentException("The SDK requires a list of supported code challenge methods when PKCE is required");
        }
        if (!codeChallengeMethodsSupported.isEmpty() && codeChallengeMethodsSupported.retainAll(Collections.singleton("S256"))) {
            throw new IllegalArgumentException("The SDK only supported code challenge method S256");
        }
    }

    protected void validateList(String property, boolean required, Collection<String> list, String... acceptedValues) {
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
            Set<String> accepted = Set.of(acceptedValues);
            if (!accepted.containsAll(list)) {
                throw new IllegalArgumentException("The SDK detected illegal values in list of values for %s.".formatted(property));
            }
        }
    }

    public boolean supportsAuthorizationDetailsType(String type) {
        return getAuthorizationDetailsTypesSupported().contains(type);
    }

    /**
     * Gets calculated endpoint uri for OpenID Connect discovery endpoint.
     */
    public URI getOidcDiscoveryEndpoint() {
        return URIUtils.appendPath(getIssuer(), "/.well-known/openid-configuration");
    }

}
