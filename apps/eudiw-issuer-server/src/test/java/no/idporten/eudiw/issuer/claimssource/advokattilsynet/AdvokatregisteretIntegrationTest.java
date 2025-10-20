package no.idporten.eudiw.issuer.claimssource.advokattilsynet;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import no.idporten.eudiw.issuer.claimssource.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ActiveProfiles("junit")
@SpringBootTest
public class AdvokatregisteretIntegrationTest {

    @MockitoBean(name="advokatregisteretMaskinportenClient")
    private MaskinportenClient maskinportenClient;

    @Autowired
    private AdvokatregisteretProperties advokatregisteretProperties;


    private AdvokatregisteretIntegration advokatregisteretIntegration;

    private MockServerRestClientCustomizer customizer;

    @BeforeEach
    public void setUp() {
        when(maskinportenClient.getAccessToken(anyString(), anyList())).thenReturn(mock(AccessToken.class));
        customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        advokatregisteretIntegration = new AdvokatregisteretIntegration(advokatregisteretProperties, maskinportenClient, builder.build());
    }

    @Test
    void test200ResponseWithDataReturnsPersonPrivate() {
        final String validResponse = """
                 {
                        "regnr": "47756",
                        "tittel": "Advokat",
                        "fornavn": "F",
                        "mellomnavn": "M",
                        "etternavn": "E",
                        "tilknyttedePraksiser": [
                        {
                            "organisasjonsNummer": 314259173,
                            "hovedpraksis": true
                        }
                    ]
                    }""";
        final String personIdentifier = "123456789101";
        customizer.getServer()
                .expect(requestToUriTemplate("?subject={personIdentifier}&envelope=false", personIdentifier))
                .andRespond(withSuccess(validResponse, MediaType.APPLICATION_JSON));
        PersonPrivate personPrivate = advokatregisteretIntegration.retrieve(personIdentifier);
        assertAll(
                () -> assertEquals("Advokat", personPrivate.tittel()),
                () -> assertEquals("47756", personPrivate.regnr()),
                () -> assertEquals("F", personPrivate.fornavn()),
                () -> assertEquals("M", personPrivate.mellomnavn()),
                () -> assertEquals("E", personPrivate.etternavn()),
                () -> assertEquals(1, personPrivate.tilknyttedePraksiser().size()),
                () -> assertEquals(314259173, personPrivate.tilknyttedePraksiser().getFirst().organisasjonsNummer()),
                () -> assertTrue(personPrivate.tilknyttedePraksiser().getFirst().hovedpraksis())
        );
    }

    @Test
    void testEmptyResponseGivesDataNotFoundException() {
        final String personIdentifier = "123456789101";
        customizer.getServer()
                .expect(requestToUriTemplate("?subject={personIdentifier}&envelope=false", personIdentifier))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        ClaimsSourceDataNotFoundException e = assertThrows(
                ClaimsSourceDataNotFoundException.class,
                () -> advokatregisteretIntegration.retrieve(personIdentifier));
        assertAll(
                () -> assertEquals("credential_issuance_denied", e.getError()),
                () -> assertTrue(e.getErrorDescription().contains("No data available"))
        );
    }

    @Test
    void test4xxResponseGivesClaimsSourceException() {
        final String errorReponse = """
                {
                    "code": 1024,
                    "description": "There was an error with the provided access token: SecurityTokenExpiredException: IDX10223: Lifetime validation failed. The token is expired. ValidTo (UTC): '10/2/2025 6:17:33 AM', Current time (UTC): '10/3/2025 8:11:41 AM'.",
                    "invocationId": "16e389a6-b768-4f8b-8a7e-22f2b8844642"
                }
                """;
        final String personIdentifier = "123456789101";
        customizer.getServer()
                .expect(requestToUriTemplate("?subject={personIdentifier}&envelope=false", personIdentifier))
                .andRespond(withUnauthorizedRequest().body(errorReponse).contentType(MediaType.APPLICATION_JSON));
        ClaimsSourceException e = assertThrows(
                ClaimsSourceException.class,
                () -> advokatregisteretIntegration.retrieve(personIdentifier));
        assertAll(
                () -> assertEquals("server_error", e.getError()),
                () -> assertEquals("Failed to get information from Advokatregisteret", e.getErrorDescription())
        );
    }

}
