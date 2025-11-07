package no.idporten.eudiw.verifier.proxy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("junit")
@SpringBootTest
class VerifierProxyApplicationTest {

    @Test
    @DisplayName("Verify that Spring contexts are loaded without exceptions")
    void contextLoads() {
    }
}