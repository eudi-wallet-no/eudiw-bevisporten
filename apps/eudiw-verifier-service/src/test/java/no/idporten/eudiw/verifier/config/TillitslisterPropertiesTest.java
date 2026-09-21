package no.idporten.eudiw.verifier.config;


import no.idporten.eudiw.verifier.trustlist.TrustlistsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.List;

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
                () -> assertFalse(properties.getAttestationTrustlists().isEmpty()),
                () -> assertFalse(properties.getPidTrustlists().isEmpty())
        );
    }

    @Test
    void tillitslisterEidas2SandkassePropertiesContainExpectedContent() {
        assertAll(
                () -> assertEquals(
                        List.of(URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_tsl.xtsl")),
                        properties.getAttestationTrustlists()),
                () -> assertEquals(
                        List.of(
                                URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_pid.jws"),
                                URI.create("https://trustlist.webuild.jwt")),
                        properties.getPidTrustlists())
        );
    }

    @Test
    @DisplayName("with configured issuer URLs, then matching and default trustlists are returned")
    void issuerTrustlistsAreResolvedWithDefault() {
        assertAll(
                () -> assertEquals(
                        URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_pid.jws"),
                        properties.getTrustlistForIssuer("https://utsteder.eidas2sandkasse.dev/pid")),
                () -> assertEquals(
                        URI.create("https://trustlist.webuild.jwt"),
                        properties.getTrustlistForIssuer("https://utsteder.eidas2sandkasse.dev/webuild")),
                () -> assertEquals(
                        URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_tsl.xtsl"),
                        properties.getTrustlistForIssuer("https://unknown.example/issuer"))
        );
    }
}
