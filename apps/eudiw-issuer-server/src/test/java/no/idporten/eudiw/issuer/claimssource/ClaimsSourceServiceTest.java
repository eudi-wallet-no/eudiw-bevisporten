package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.ExtendedCredentialConfiguration;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When looking up claims sources")
@ActiveProfiles("junit")
@SpringBootTest
public class ClaimsSourceServiceTest {

    @Autowired
    ClaimsSourceService claimsSourceService;

    @Autowired
    CredentialIssuerServerProperties credentialIssuerServerProperties;

    @MockitoBean
    AuditLogger auditLogger;

    private ClaimsSource findClaimsSourceByCredentialType(String credentialType) {
        ExtendedCredentialConfiguration ccp = credentialIssuerServerProperties.getCredentialConfigurations().stream()
                .filter(credentialConfiguration -> credentialConfiguration.getCredentialType().equals(credentialType))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("server_error", "Unknown credential type [%s]".formatted(credentialType), HttpStatus.INTERNAL_SERVER_ERROR));
        return claimsSourceService.findClaimsSource(ccp.getCredentialIssuerContext().getCredentialDataSourceUri());
    }

    @DisplayName("then the same claims source can be used for several credential configurations")
    @Test
    void testClaimsSourceSupportsMultipleCredentialTypes() {
        ClaimsSource claimsSourceForSdJWT = findClaimsSourceByCredentialType("urn:junitdoc-pre");
        ClaimsSource claimsSourceForMdoc = findClaimsSourceByCredentialType("junitdoc-pre");
        assertAll(
                () -> assertNotNull(claimsSourceForSdJWT),
                () -> assertSame(claimsSourceForSdJWT, claimsSourceForMdoc)
        );
    }

        @DisplayName("then an exception is thrown when looking up an unknown claims source")
        @Test
        void testGetMetadataNonExistingClaimsSource() {
            assertThrows(IssuerServerException.class, () -> claimsSourceService.findClaimsSource(URI.create("class://non-existing-claims-source")));
        }

}
