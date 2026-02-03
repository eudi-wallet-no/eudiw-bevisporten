package no.idporten.eudiw.issuer.authoritativesources.krr;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When setting properties for krr")
@ActiveProfiles("junit")
@SpringBootTest
public class KrrPropertiesTest {

    @Autowired
    KrrProperties krrProperties;

    @DisplayName("when setting properties for krr")
    @Test
    void testThePropertiesSetForKrr() {
        assertAll(
                () -> assertEquals(Duration.ofSeconds(2), krrProperties.readTimeout()),
                () -> assertEquals(Duration.ofSeconds(2), krrProperties.connectTimeout()),
                () -> assertEquals("krr:global/kontaktinformasjon.read", krrProperties.scope()),
                () -> assertEquals(new URI("https://test.kontaktregisteret.no/"), krrProperties.uri()),
                () -> assertEquals(1, krrProperties.scopeAsList().size())
        );
    }

    @Test
    @DisplayName("When having more scopes and testing the list of scopes")
    void testTheScopesMethod() {
        KrrProperties properties = new KrrProperties(krrProperties.uri(),
                "test:scope test:scope2",
                Duration.ofSeconds(2),
                Duration.ofSeconds(2));

        assertAll(
                () -> assertEquals(2, properties.scopeAsList().size()),
                () -> assertEquals("test:scope", properties.scopeAsList().get(0)),
                () -> assertNotEquals("test:scope2", properties.scopeAsList().get(0)),
                () -> assertEquals("test:scope2", properties.scopeAsList().get(1)),
                () -> assertNotEquals(1, properties.scopeAsList().size())
        );
    }

    @Test
    @DisplayName("When having one scope")
    void testOneScope() {
        KrrProperties properties = new KrrProperties(krrProperties.uri(),
                "one:scope", Duration.ofSeconds(2), Duration.ofSeconds(2));
        assertAll(
                () -> assertEquals("one:scope", properties.scope()),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> properties.scopeAsList().get(1)),
                () -> assertEquals(1, properties.scopeAsList().size()),
                () -> assertNotEquals(2, properties.scopeAsList().size())
        );
    }
}
