package no.idporten.eudiw.issuer.ui.issuer;

import no.idporten.eudiw.issuer.ui.issuer.config.IssuerServerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("junit")
@SpringBootTest
public class IssuerServerServiceTest {

    @Autowired
    private IssuerServerService issuerServerService;

    @Autowired
    private IssuerServerProperties issuerServerProperties;

    @Test
    void testCreateCredentialOfferRequestUri() {
        URI uri = issuerServerService.createCredentialOfferRequestUri(issuerServerProperties.findCredentialConfiguration("test-id"));
        assertEquals("https://my-unit-test-issuer-server/junit/api/v1/credential-offer/create?credential_configuration_id=test-id", uri.toString());
    }

}
