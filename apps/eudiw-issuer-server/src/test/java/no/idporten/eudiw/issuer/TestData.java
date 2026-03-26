package no.idporten.eudiw.issuer;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.config.ClasspathSingleCredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialConfigurationSourceProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

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
                .build();
    }

    public static ExtendedCredentialConfiguration credentialConfigurationFromClasspath(String uri) {
        CredentialConfigurationSource credentialConfigurationSource = new ClasspathSingleCredentialConfigurationSource(new CredentialConfigurationSourceProperties(uri, null, null, null, null));
        credentialConfigurationSource.init();
        return credentialConfigurationSource.retrieve().getFirst();
    }

    /**
     * A plain access_token with pid claim
     */
    public static JWT accessToken(String personIdentifier) {
        return new PlainJWT(new JWTClaimsSet.Builder().claim("pid", personIdentifier).build());
    }

    /**
     * A plain access_token with pid and tx_id claim
     */
    public static JWT accessToken(String personIdentifier, IssuanceTransactionId txId) {
        return new PlainJWT(new JWTClaimsSet.Builder()
                .claim("pid", personIdentifier)
                .claim("tx_id", txId.getValue())
                .build());
    }

    public static String syntheticPersonIdentifier() {
        return "03866599717";
    }

}
