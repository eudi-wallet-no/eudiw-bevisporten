package no.idporten.eudiw.issuer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("When creating endpoint URIs")
public class EndpointsTest {

    @DisplayName("then tenants should be correctly substituted in the URI")
    @Test
    void testGenerateEndpointUriForTenant() {
        URI endpointUri = Endpoints.endpointURI(URI.create("https://junit.eidas2sandkasse.dev"), Endpoints.CREDENTIAL_ENDPOINT_TENANT, "abc");
        assertAll(
                () -> assertEquals("https://junit.eidas2sandkasse.dev/abc/openid4vci/credential", endpointUri.toString()),
                () -> assertEquals("junit.eidas2sandkasse.dev", endpointUri.getHost()),
                () -> assertEquals("/abc/openid4vci/credential", endpointUri.getPath())
        );
    }

    @DisplayName("then the root tenant should be correctly substituted in the URI")
    @Test
    void testGenerateEndpointUriForRootTenant() {
        URI endpointUri = Endpoints.endpointURI(URI.create("https://junit.eidas2sandkasse.dev"), Endpoints.CREDENTIAL_ENDPOINT_TENANT, "root");
        assertAll(
                () -> assertEquals("https://junit.eidas2sandkasse.dev/openid4vci/credential", endpointUri.toString()),
                () -> assertEquals("junit.eidas2sandkasse.dev", endpointUri.getHost()),
                () -> assertEquals("/openid4vci/credential", endpointUri.getPath())
        );
    }

    @DisplayName("then the root tenant null should be correctly substituted in the URI")
    @Test
    void testGenerateEndpointUriForRootTenantAsNull() {
        URI endpointUri = Endpoints.endpointURI(URI.create("https://junit.eidas2sandkasse.dev"), Endpoints.CREDENTIAL_ENDPOINT_TENANT, null);
        assertAll(
                () -> assertEquals("https://junit.eidas2sandkasse.dev/openid4vci/credential", endpointUri.toString()),
                () -> assertEquals("junit.eidas2sandkasse.dev", endpointUri.getHost()),
                () -> assertEquals("/openid4vci/credential", endpointUri.getPath())
        );
    }


}
