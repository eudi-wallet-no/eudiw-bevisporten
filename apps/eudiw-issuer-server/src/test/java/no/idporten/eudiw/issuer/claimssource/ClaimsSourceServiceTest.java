package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.authoritativesources.JUnitClaimsSource;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("junit")
@SpringBootTest
public class ClaimsSourceServiceTest {

    @Autowired
    ClaimsSourceService claimsSourceService;

    @MockitoBean
    AuditLogger auditLogger;

    @Test
    void testLoadAndInitClaimsSources() {
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource("junitdoc");
        assertAll(
                () -> assertInstanceOf(JUnitClaimsSource.class, claimsSource),
                () -> assertNotNull(claimsSource.getProperties())
        );
    }


    @Test
    void testClaimsSourceSupportsMultipleCredentialTypes() {
        ClaimsSource claimsSourceForSdJWT = claimsSourceService.findClaimsSource("urn:junitdoc-pre");
        ClaimsSource claimsSourceForMdoc = claimsSourceService.findClaimsSource("junitdoc-pre");
        assertAll(
                () -> assertNotNull(claimsSourceForSdJWT),
                () -> assertSame(claimsSourceForSdJWT, claimsSourceForMdoc)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"junitdoc", "urn:junitdoc-pre"})
    void testGetMetadataExists(String credentialType) {
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialType);
        ClaimsSourceMetadata metadata = claimsSourceService.getMetadata(claimsSource, new CredentialConfigurationProperties());

        assertAll(
                () -> assertNotNull(metadata),
                () -> assertNotNull(metadata.getDisplays()),
                () -> assertFalse(metadata.getDisplays().isEmpty()),
                () -> assertNotNull(metadata.getClaims()),
                () -> assertFalse(metadata.getClaims().isEmpty()),
                () -> assertTrue(metadata.getClaims().stream()
                        .allMatch(claim -> claim.getPath() != null)),
                () -> assertTrue(metadata.getClaims().stream()
                        .anyMatch(ClaimsDescription::isMandatory))
        );
    }


}
