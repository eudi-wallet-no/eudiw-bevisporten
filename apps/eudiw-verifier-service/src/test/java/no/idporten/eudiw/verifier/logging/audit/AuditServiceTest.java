package no.idporten.eudiw.verifier.logging.audit;

import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("When audit logging is used")
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AuditService auditService;

    @DisplayName("then nothing is logged when the service is created")
    @Test
    void testNothingLoggedOnCreate() {
        assertNotNull(auditService);
        verifyNoInteractions(auditLogger);
    }

}
