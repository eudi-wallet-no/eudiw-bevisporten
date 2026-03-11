package no.idporten.eudiw.connector.authoritativsources.krr;


import no.idporten.eudiw.connector.authoritativsources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativsources.api.Subject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When getting a KrrAuthoritativeSource")
public class KrrAuthoritativeSourceTest {
    private final KrrAuthoritativeSource krrAuthoritativeSource =  new KrrAuthoritativeSource();


    @Test
    @DisplayName("then it should contain specific keys")
    void getKrrAuthoritativeSource() {
        Subject subject = new Subject("12345");
        CredentialData a = krrAuthoritativeSource.retrieveCredentialData(subject);

        assertTrue(a.containsKey("personidentifikator"));
        assertTrue(a.containsKey("mobiltelefonnummer"));
        assertTrue(a.containsKey("epostadresse"));
        assertEquals(a.get("personidentifikator"), subject.identifier());
    }
}
