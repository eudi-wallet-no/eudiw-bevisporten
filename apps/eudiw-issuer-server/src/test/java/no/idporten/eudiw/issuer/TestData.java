package no.idporten.eudiw.issuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.issuer.config.ClasspathCredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.LocalResourceProperties;
import no.idporten.eudiw.issuer.credentials.configurations.CredentialIssuerContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import no.idporten.eudiw.issuer.openid4vci.proofs.BindingKeys;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Class helping with test data.
 */
public class TestData {

    /**
     * Creates a credential issuer tenant with the given tenant id and a credential issuer URI based on the tenant id.
     */
    public static CredentialIssuerTenant credentialIssuerTenant(String tenant) {
        return CredentialIssuerTenant.builder().id(tenant).credentialIssuer(URI.create("http://junit-issuer.eidas2sandkasse.dev/%s".formatted(tenant))).build();
    }

    /**
     * A default credential issuer for junit tests.
     */
    public static CredentialIssuerTenant junitIssuerTenant() {
        return credentialIssuerTenant("junit");
    }

    /**
     * A default and very plain credential configuration for junit tests.
     */
    public static ExtendedCredentialConfiguration junitCredentialConfiguration() {
        return ExtendedCredentialConfiguration.builder()
                .credentialConfigurationId("junit-ccid")
                .extendedCredentialMetadata(new ExtendedCredentialMetadata(
                        List.of(new Display("no", "Junit doc", "Kun for junit-tester")),
                        List.of(new ExtendedClaimsDescription(null, "c", ClaimDataType.STRING, Map.of("no", "c1"), true, ".*"))))
                .credentialIssuerContext(new CredentialIssuerContext())
                .build();
    }

    public static ExtendedCredentialConfiguration credentialConfigurationFromClasspath(String uri) {
        CredentialConfigurationSource credentialConfigurationSource = new ClasspathCredentialConfigurationSource(new LocalResourceProperties(List.of(URI.create(uri))));
        credentialConfigurationSource.init();
        return credentialConfigurationSource.retrieve().getFirst();
    }

    /**
     * A plain access_token with subject claim
     */
    public static JWT accessToken(String subject) {
        return new PlainJWT(new JWTClaimsSet.Builder().subject(subject).build());
    }

    /**
     * A plain access_token for pre-authorization with pid and tx_id claim
     */
    public static JWT preAuthAccessToken(String personIdentifier, IssuanceTransactionId txId) {
        return new PlainJWT(new JWTClaimsSet.Builder()
                .claim("pid", personIdentifier)
                .claim("tx_id", txId.getValue())
                .build());
    }

    public static String syntheticPersonIdentifier() {
        return "03866599717";
    }

    /**
     * Binding keys without any validated proofs
     */
    public static BindingKeys bindingKeys() {
        return new BindingKeys(List.of());
    }

    /**
     * A signed wallet instance attestation.
     */
    public static SignedJWT walletInstanceAttestationJwt() {
        return walletInstanceAttestationJwt(new JWTClaimsSet.Builder()
                .claim("wallet_name", "junit-wallet")
                .claim("client_status", Map.of(
                        "status", Map.of("status_list", Map.of(
                                "idx", 1337,
                                "uri", "https://junit-wallet-provider.example/wia-statuslists/42")),
                        "exp", 4102444800L))
                .build());
    }

    /**
     * A signed wallet instance attestation with the given claims.
     */
    public static SignedJWT walletInstanceAttestationJwt(JWTClaimsSet claims) {
        try {
            SignedJWT wia = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), claims);
            wia.sign(new ECDSASigner(new ECKeyGenerator(Curve.P_256).generate()));
            return wia;
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to create wallet instance attestation", e);
        }
    }

}
