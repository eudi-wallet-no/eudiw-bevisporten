package no.idporten.eudiw.oauth2.server;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.dpop.DPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import no.idporten.eudiw.oauth2.server.cache.SimpleOpenIDConnectCache;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.util.JsonObjectBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.text.ParseException;
import java.util.*;

/**
 * Utilities for writing tests.
 */
public class TestUtils {

    private final static String defaultIssuer = "http://my-test-server";

    /**
     * A default client for testing.
     */
    public static ClientMetadata defaultClientMetadata() {
        return defaultClientMetadataBuilder().build();
    }

    /**
     * A builder creating a default client for testing.
     */
    public static ClientMetadata.ClientMetadataBuilder defaultClientMetadataBuilder() {
        return ClientMetadata.builder()
                .clientId("cid")
                .redirectUri("https://idporten.no/")
                .redirectUri("https://2.idporten.no/")
                .scope("openid");
    }

    /**
     * A default issuer for testing.
     */
    public static String defaultIssuer() {
        return defaultIssuer;
    }

    /**
     * A default api key for testing.
     */
    public static String defaultApiKey() {
        return "junit-api-key";
    }

    public static MultiValueMap<String, String> headersWithApiKey(String apiKey) {
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("X-API-KEY", apiKey);
        return headers;
    }

    /**
     * A default server configuration for testing.
     */
    public static OAuth2ServerConfiguration defaultEmbeddedServerTestConfiguration() throws Exception {
        return defaultOAuth2ServerTestConfigurationBuilder().build();
    }

    /**
     * A builder for default server configuration for testing.
     */
    public static OAuth2ServerConfiguration.OAuth2ServerConfigurationBuilder defaultOAuth2ServerTestConfigurationBuilder() throws Exception {
        return OAuth2ServerConfiguration.builder()
                .apiKey(defaultApiKey())
                .issuer(new URI(defaultIssuer()))
                .grantTypesSupported(List.of("urn:ietf:params:oauth:grant-type:pre-authorized_code", "authorization_code"))
                .cache(new SimpleOpenIDConnectCache())
                .jwk(new ECKeyGenerator(Curve.P_256)
                        .keyUse(KeyUse.SIGNATURE)
                        .keyID("test-kid")
                        .generate())
                .client(defaultClientMetadata())
                .acrValue("Level3")
                .acrValue("Level4")
                .uiLocale("nn");
    }

    public static String createDpopHeader(URI tokenEndpoint) throws JOSEException {
        DPoPProofFactory proofFactory = getDPoPProofFactory();
        SignedJWT dPopProof = proofFactory.createDPoPJWT("POST", tokenEndpoint);
        return "%s.%s.%s".formatted(dPopProof.getHeader().toBase64URL(), dPopProof.getPayload().toBase64URL(), dPopProof.getSignature());
    }

    public static String createDpopHeader(URI tokenEndpoint, DPoPProofFactory dPoPProofFactory) throws JOSEException {
        SignedJWT dPopProof = dPoPProofFactory.createDPoPJWT("POST", tokenEndpoint);
        return "%s.%s.%s".formatted(dPopProof.getHeader().toBase64URL(), dPopProof.getPayload().toBase64URL(), dPopProof.getSignature());
    }

    public static DPoPProofFactory getDPoPProofFactory() throws JOSEException {
        ECKey clientJWK = new ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate();
        JWSAlgorithm jwsAlg = JWSAlgorithm.ES256;
        return new DefaultDPoPProofFactory(clientJWK, jwsAlg);
    }

    public static String createDpopJtk() throws JOSEException {
        ECKey clientJWK = new ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate();
        return clientJWK.computeThumbprint().toString();
    }

    public static String findDpopJktFromDpopHeader(String dPoPHeader) throws JOSEException, ParseException {
        SignedJWT dPopProof = SignedJWT.parse(dPoPHeader);
        JWK jwk = dPopProof.getHeader().getJWK();
        Base64URL thumbprint = jwk.toPublicJWK().computeThumbprint();
        return thumbprint.toString();
    }

    public static ECKey createECPrivateKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).generate();
    }

    public static ECKey clientAttesterJWK() throws ParseException {
        String jwk = """
                {
                    "kty": "EC",
                    "d": "2-l5GM-TAWasqjRY3MyMRvY6e66NvTCz6JbGzaGt1Rw",
                    "use": "sig",
                    "crv": "P-256",
                    "kid": "client-attester",
                    "x": "HYcDXOZC5_-HkTQbLNchOVzURImyNIjBCxPxJ_bDq1c",
                    "y": "TLXo-M78sfQ1ka3xQ0ifoS1FlJbC3MvHB4Amt3gjoZU",
                    "alg": "ES256",
                    "x5c": ["MIIBJjCBzKADAgECAgYBnhtoXDQwCgYIKoZIzj0EAwIwGjEYMBYGA1UEAwwPY2xpZW50LWF0dGVzdGVyMB4XDTI2MDUxMjA4NTgwNFoXDTI3MDMwODA4NTgwNFowGjEYMBYGA1UEAwwPY2xpZW50LWF0dGVzdGVyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEHYcDXOZC5/+HkTQbLNchOVzURImyNIjBCxPxJ/bDq1dMtej4zvyx9DWRrfFDSJ+hLUWUlsLcy8cHgCa3eCOhlTAKBggqhkjOPQQDAgNJADBGAiEAgGImI4eYd52Qqq6Q4aa/H3sk3Jqj7hRKocmYLJ1IEUsCIQDL4qJBw6JHFnBX1vEDbxmWuR4inapgOcsv4WtP+YKpJA=="]
                }""";
        return ECKey.parse(jwk);
    }

    public static SignedJWT createClientAttestation(String clientId, String walletName, String walletLink, ECKey clientKey) throws ParseException, JOSEException {
        return createClientAttestation(clientId, walletName, walletLink, clientKey, false, true);
    }

    public static SignedJWT createClientAttestation(String clientId, String walletName, String walletLink, ECKey clientKey, boolean includeJwk, boolean includeX5c) throws ParseException, JOSEException {
        ECKey clientAttesterJWK = clientAttesterJWK();
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("oauth-client-attestation+jwt"));
        if (includeJwk) {
            headerBuilder.jwk(clientAttesterJWK.toPublicJWK());
        }
        if (includeX5c) {
            headerBuilder.x509CertChain(clientAttesterJWK.getX509CertChain());
        }
        SignedJWT clientAttestation =
                new SignedJWT(
                        headerBuilder.build(),
                        new JWTClaimsSet.Builder()
                                .issuer("client-attester")
                                .audience("anyone")
                                .subject(clientId)
                                .jwtID(UUID.randomUUID().toString())
                                .expirationTime(new Date(new Date().getTime() + 1000 * 60 * 60 * 24))
                                .claim("cnf", JsonObjectBuilder.builder().addAttribute("jwk", clientKey.toPublicJWK().toJSONObject()).build())
                                .claim("wallet_name", walletName)
                                .claim("wallet_link", walletLink)
                                .build());
        clientAttestation.sign(new ECDSASigner(clientAttesterJWK.toECPrivateKey()));
        return clientAttestation;
    }

    public static SignedJWT createClientAttestationPoP(String clientId, String challenge, String audience, ECKey clientKey) throws ParseException, JOSEException {
        SignedJWT clientAttestationPop =
                new SignedJWT(
                        JWSHeader.parse("""
                                {
                                  "typ": "oauth-client-attestation-pop+jwt",
                                  "alg": "ES256"
                                }"""),
                        new JWTClaimsSet.Builder()
                                .issuer(clientId)
                                .audience(audience)
                                .jwtID(UUID.randomUUID().toString())
                                .claim("challenge", challenge)
                                .build());
        clientAttestationPop.sign(new ECDSASigner(clientKey.toECPrivateKey()));
        return clientAttestationPop;
    }

    public static SignedJWT createClientAttestationPoPWithoutChallenge(String clientId, String audience, ECKey clientKey) throws ParseException, JOSEException {
        SignedJWT clientAttestationPop =
                new SignedJWT(
                        JWSHeader.parse("""
                                {
                                  "typ": "oauth-client-attestation-pop+jwt",
                                  "alg": "ES256"
                                }"""),
                        new JWTClaimsSet.Builder()
                                .issuer(clientId)
                                .audience(audience)
                                .jwtID(UUID.randomUUID().toString())
                                .build());
        clientAttestationPop.sign(new ECDSASigner(clientKey.toECPrivateKey()));
        return clientAttestationPop;
    }
}

