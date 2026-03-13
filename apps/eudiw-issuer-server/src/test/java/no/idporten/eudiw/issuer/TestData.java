package no.idporten.eudiw.issuer;

import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;

import java.net.URI;

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

}
