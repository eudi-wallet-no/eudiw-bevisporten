package no.idporten.eudiw.issuer;

import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("junit")
@SpringBootTest
class IssuerServerApplicationTests {

    @MockitoBean
    private AuditLogger auditLogger;

	@Test
	void contextLoads() {
	}

}
