package no.idporten.eudiw.verifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("junit")
@SpringBootTest
class VerifierServiceApplicationTest {

    @Test
    @DisplayName("Verify that Spring contexts are loaded without exceptions")
    void contextLoads() {
    }
}