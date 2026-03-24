package no.idporten.eudiw.connector.authoritativesources.krr;


import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.connector.authoritativesources.krr.model.Kontaktinformasjon;
import no.idporten.eudiw.connector.authoritativesources.krr.model.PersonKrr;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.idporten.eudiw.connector.authoritativesources.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("When getting a KrrAuthoritativeSource")
public class KrrCredentialDataSourceTest {

    @Mock
    KrrIntegration krrIntegration;

    @InjectMocks
    private KrrCredentialDataSource krrAuthoritativeSource;


    @Test
    @DisplayName("then data can be pulled from authoritative source")
    void testPullFromAuthoritativeSource() {
        PersonKrr personKrr = getValidPersonKrr();
        when(krrIntegration.retrieve(any())).thenReturn(personKrr);

        Subject subject = getValidSubject();
        String personIdentifier = subject.identifier();

        CredentialData data = krrAuthoritativeSource.retrieveCredentialData(subject);

        assertAll(
                () -> assertNotNull(data),
                () -> assertEquals(3, data.size()),
                () -> assertEquals(personIdentifier, data.get("personidentifikator")),
                () -> assertEquals(personKrr.kontaktinformasjon().mobiltelefonnummer(), data.get("mobiltelefonnummer")),
                () -> assertEquals(personKrr.kontaktinformasjon().epostadresse(), data.get("epostadresse"))
        );

    }

    @Test
    @DisplayName("When personKrr is null, throws correct exception")
    void testNullPersonKrrThrowsCorrectException() {
        Subject subject = getValidSubject();
        assertThrows(ClaimsSourceDataNotFoundException.class, () -> krrAuthoritativeSource.retrieveCredentialData(subject));
    }


    @Test
    @DisplayName("When personKrr contact information is outdated, throws correct exception")
    void testPersonKrrThrowsCorrectExceptionWhenOutdatedContactInformation() {
        Subject subject = getValidSubject();
        PersonKrr personKrr = getCustomPersonKrr(subject.identifier(), "NEI", "AKTIV", "KAN_IKKE_VARSLES", getValidKontaktinformasjon());
        when(krrIntegration.retrieve(subject.identifier())).thenReturn(personKrr);

        assertThrows(ClaimsSourceInvalidDataException.class, () -> krrAuthoritativeSource.retrieveCredentialData(subject));
    }


    @Test
    @DisplayName("When personKrr is reserved, throws correct exception")
    void testPersonKrrThrowsCorrectExceptionWhenReserved() {
        Subject subject = getValidSubject();
        PersonKrr personKrr = getCustomPersonKrr(subject.identifier(), "JA", "AKTIV", "KAN_VARSLES", getValidKontaktinformasjon());
        when(krrIntegration.retrieve(subject.identifier())).thenReturn(personKrr);

        assertThrows(ClaimsSourceInvalidDataException.class, () -> krrAuthoritativeSource.retrieveCredentialData(subject));
    }


    @Test
    @DisplayName("When personKrr has not registered telephone and email")
    void testPersonKrrThrowsCorrectExceptionWhenNotTelephoneNumberOrEmailIsRegistered() {
        Subject subject = getValidSubject();
        PersonKrr personKrr = getCustomPersonKrr(subject.identifier(), "JA", "AKTIV", "KAN_VARSLES", new Kontaktinformasjon("", ""));
        when(krrIntegration.retrieve(subject.identifier())).thenReturn(personKrr);

        assertThrows(ClaimsSourceInvalidDataException.class, () -> krrAuthoritativeSource.retrieveCredentialData(subject));
    }
}
