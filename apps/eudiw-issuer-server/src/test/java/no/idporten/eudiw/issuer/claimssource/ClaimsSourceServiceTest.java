package no.idporten.eudiw.issuer.claimssource;


import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.Test;
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
                () -> assertTrue(claimsSource instanceof JUnitClaimsSource),
                () -> assertNotNull(claimsSource.getProperties())
        );
    }

}
