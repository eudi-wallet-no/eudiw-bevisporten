package no.idporten.eudiw.verifier.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When loading application properties for SD-JWT configuration")
public class SdjwtConfigTest {
    @Autowired
    private SdjwtConfig sdjwtConfig;

    @Test
    @DisplayName("Then the SD-JWT configuration properties should be loaded and not null")
    void clockSkewSdJwtPropertyTest() {
        assertAll(
                () -> assertNotNull(sdjwtConfig),
                () -> assertNotNull(sdjwtConfig.clockSkew()),
                () -> assertEquals(java.time.Duration.ofSeconds(30), sdjwtConfig.clockSkew()));
    }
}
