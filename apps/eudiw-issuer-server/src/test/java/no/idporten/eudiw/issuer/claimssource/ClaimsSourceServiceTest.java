package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @DisplayName("then the HTTP pull claims source can be looked up by a http(s):// uri")
    @ValueSource(strings = {"http://junit.eidas2sandkasse.dev/authsource", "https://junit.eidas2sandkasse.dev/authsource"})
    @ParameterizedTest
    void testFindClaimsSourceForHttpUri(String uri) {
        assertSame(HttpPullPreAuthorizedClaimsSource.class, claimsSourceService.findClaimsSource(URI.create(uri)).getClass());
    }

    @DisplayName("then an exception is thrown when looking up an unknown claims source")
    @Test
    void testFindNonExistingClaimsSource() {
        assertThrows(IssuerServerException.class, () -> claimsSourceService.findClaimsSource(URI.create("class://non-existing-claims-source")));
    }

}
