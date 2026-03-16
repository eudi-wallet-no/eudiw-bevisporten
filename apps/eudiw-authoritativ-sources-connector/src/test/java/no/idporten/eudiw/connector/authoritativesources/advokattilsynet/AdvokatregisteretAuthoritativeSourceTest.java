package no.idporten.eudiw.connector.authoritativesources.advokattilsynet;


import no.idporten.eudiw.connector.authoritativesources.TestData;
import no.idporten.eudiw.connector.authoritativesources.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceDataNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@DisplayName("When issuing documents for Advokatregisteret")
@ExtendWith(MockitoExtension.class)
public class AdvokatregisteretAuthoritativeSourceTest {
    @Mock
    AdvokatregisteretIntegration advokatregisteretIntegration;

    @InjectMocks
    AdvokatregisteretAuthoritativeSource claimsSource;


    @DisplayName("then data can be pulled from authoritative source")
    @Test
    void testPullFromAuthoritativeSource() {
        Subject subject = new Subject("12345678901");
        String response = """
                {
                    "regnr": "47756",
                    "tittel": "Advokat",
                    "fornavn": "ØKONOMISK INITIATIVRIK",
                    "mellomnavn": null,
                    "etternavn": "FOT",
                    "tilknyttedePraksiser": [
                        {
                            "organisasjonsNummer": 314259173,
                            "hovedpraksis": false
                        }
                    ]
                }""";
        PersonPrivate personPrivate = new JsonMapper().readValue(response, PersonPrivate.class);
        when(advokatregisteretIntegration.retrieve(eq(subject.identifier()))).thenReturn(personPrivate);
        CredentialData credentialData = claimsSource.retrieveCredentialData(subject);
                assertAll(
                () -> assertNotNull(credentialData),
                () -> assertEquals(5, credentialData.size()),
                () -> assertEquals(subject.identifier(), credentialData.get("personidentifikator")),
                () -> assertEquals("47756", credentialData.get("regnr")),
                () -> assertEquals("Advokat", credentialData.get("tittel")),
                () -> assertEquals("ØKONOMISK INITIATIVRIK", credentialData.get("fornavn")),
                () -> assertEquals("FOT", credentialData.get("etternavn")),
                () -> assertFalse(credentialData.containsKey("mellomnavn"))
        );
    }

    @Test
    void testEmptyResponseGivesDataNotFoundException() {
        Subject subject = TestData.getValidSubject();

        when(advokatregisteretIntegration.retrieve(subject.identifier())).thenReturn(null);

        ClaimsSourceDataNotFoundException e = assertThrows(
                ClaimsSourceDataNotFoundException.class,
                () -> claimsSource.retrieveCredentialData(subject)
        );

        assertAll(
                () -> assertEquals("credential_data_not_found", e.getErrorCode()),
                () -> assertTrue(e.getErrorDescription().contains("No data available"))
        );
    }

}
