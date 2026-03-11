package no.idporten.eudiw.connector.authoritativesources.freg;

import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When getting AgeVerificationAuthoritativeSource")
public class AgeVerificationAuthoritativeSourceTest {
    private final AgeVerificationAuthoritativeSource ageVerificationAuthoritativeSource =  new AgeVerificationAuthoritativeSource();


    @Test
    @DisplayName("then it should contain specific keys")
    void getKrrAuthoritativeSource() {
        Subject subject = new Subject("12345");
        CredentialData a = ageVerificationAuthoritativeSource.retrieveCredentialData(subject);

        assertTrue(a.containsKey("age_over_15"));
        assertTrue(a.containsKey("age_over_18"));
    }
}
