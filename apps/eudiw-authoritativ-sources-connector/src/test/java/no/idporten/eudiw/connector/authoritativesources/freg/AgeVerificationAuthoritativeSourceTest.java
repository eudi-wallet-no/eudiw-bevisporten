package no.idporten.eudiw.connector.authoritativesources.freg;

import no.digdir.freg.service.FregService;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.idporten.eudiw.connector.authoritativesources.TestData.getAgeYearOldPerson;
import static no.idporten.eudiw.connector.authoritativesources.TestData.getValidSubject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("When getting AgeVerificationAuthoritativeSource")
public class AgeVerificationAuthoritativeSourceTest {
    @Mock
    private FregService fregService = Mockito.mock(FregService.class);

    @Spy
    private PersonConverterService personConverterService = Mockito.spy(PersonConverterService.class);

    @InjectMocks
    private AgeVerificationAuthoritativeSource ageVerificationAuthoritativeSource;



    @Test
    @DisplayName("it should return age_over_15 and age_over_18 as true when person is 18 years old")
    void shouldReturnAgeOver15AndAgeOver18TrueWhenPersonIsEighteenYearsOld() {
        Subject subject = getValidSubject();

        when(fregService.getEidasPerson(subject.identifier(),"EUDIW-ISSUER")).thenReturn(getAgeYearOldPerson(18));

        CredentialData credentialData = ageVerificationAuthoritativeSource.retrieveCredentialData(subject);

        assertTrue((Boolean)credentialData.get("age_over_15"));
        assertTrue((Boolean)credentialData.get("age_over_18"));
    }

    @Test
    @DisplayName("it should return age_over_15 as true and age_over_18 as false when person is 15 years old")
    void shouldReturnAgeOver15TrueAndAgeOver18FalseWhenPersonIsFifteenYearsOld() {
        Subject subject = getValidSubject();

        when(fregService.getEidasPerson(subject.identifier(),"EUDIW-ISSUER")).thenReturn(getAgeYearOldPerson(15));

        CredentialData credentialData = ageVerificationAuthoritativeSource.retrieveCredentialData(subject);

        assertTrue((Boolean)credentialData.get("age_over_15"));
        assertFalse((Boolean)credentialData.get("age_over_18"));
    }

    @Test
    @DisplayName("it should return age_over_15 and age_over_18 as false when person is 14 years old")
    void shouldReturnAgeOver15AndAgeOver18FalseWhenPersonIsFourteenYearsOld() {
        Subject subject = getValidSubject();

        when(fregService.getEidasPerson(subject.identifier(),"EUDIW-ISSUER")).thenReturn(getAgeYearOldPerson(14));

        CredentialData credentialData = ageVerificationAuthoritativeSource.retrieveCredentialData(subject);

        assertFalse((Boolean)credentialData.get("age_over_15"));
        assertFalse((Boolean)credentialData.get("age_over_18"));
    }
}
