package no.idporten.eudiw.verifier.config;


import no.idporten.eudiw.verifier.trustlist.TrustlistsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("when loading application properties for trustlist properties")
class TillitslisterPropertiesTest {
    @Autowired
    private TrustlistsProperties properties;

    @Test
    void tillitslisterEidas2SandkassePropertiesAreNotEmpty() {
        assertAll(
                () -> assertNotNull(properties),
                () -> assertNotNull(properties.getSandboxTrustlist()),
                () -> assertNotNull(properties.getWebuildTrustlist()),
                () -> assertNotNull(properties.getSandboxTrustlist().attestations()),
                () -> assertNotNull(properties.getSandboxTrustlist().pid()),
                () -> assertNotNull(properties.getWebuildTrustlist().pid()),
                () -> assertNotNull(properties.getWebuildTrustlist().attestations())
        );
    }

    @Test
    void tillitslisterEidas2SandkassePropertiesContainExpectedContent() {
        assertAll(
                () -> assertEquals(URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_tsl.xtsl"), properties.getSandboxTrustlist().attestations()),
                () -> assertEquals(URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_pid.jws"), properties.getWebuildTrustlist().pid()),
                () -> assertEquals(URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_tsl.xtsl"), properties.getWebuildTrustlist().attestations()),
                () -> assertEquals(URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_pid.jws"), properties.getSandboxTrustlist().pid())
        );
    }
}
