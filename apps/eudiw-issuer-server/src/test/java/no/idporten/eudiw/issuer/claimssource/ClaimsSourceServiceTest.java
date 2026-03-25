package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @MockitoBean
    AuditLogger auditLogger;

    @DisplayName("then a claims source can be looked up by a class:// uri")
    @Test
    void testFindClaimsSourceForClassUri() {
        assertNotNull(claimsSourceService.findClaimsSource(URI.create("class://no.idporten.eudiw.issuer.claimssource.PushPreAuthorizedClaimsSource")));
    }

    @DisplayName("then the authoritative-source claims source can be looked up by an authoritative-source:// uri")
    @Test
    void testFindClaimsSourceForAuthoritativeSourceUri() {
        assertSame(AuthoritativeSourcePullPreAuthorizedClaimsSource.class, claimsSourceService.findClaimsSource(URI.create("authoritative-source://junit")).getClass());
    }

    @DisplayName("then an exception is thrown when looking up an unknown claims source")
    @Test
    void testFindNonExistingClaimsSource() {
        assertThrows(IssuerServerException.class, () -> claimsSourceService.findClaimsSource(URI.create("class://non-existing-claims-source")));
    }

}
