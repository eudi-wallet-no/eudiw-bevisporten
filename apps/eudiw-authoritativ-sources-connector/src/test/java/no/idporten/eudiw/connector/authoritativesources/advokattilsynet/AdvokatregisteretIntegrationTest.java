package no.idporten.eudiw.connector.authoritativesources.advokattilsynet;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import no.idporten.eudiw.connector.authoritativesources.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceException;
import no.idporten.lib.maskinporten.client.JwtGrantTokenInterceptor;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import no.idporten.lib.maskinporten.client.MaskinportenClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

@ActiveProfiles("junit")
@SpringBootTest
public class AdvokatregisteretIntegrationTest {

    @MockitoBean
    private MaskinportenClients maskinportenClients;

    @MockitoBean
    private MaskinportenClient maskinportenClient;

    @MockitoBean
    private JwtGrantTokenInterceptor jwtGrantTokenInterceptor;

    @Autowired
    private AdvokatregisteretProperties advokatregisteretProperties;

    private AdvokatregisteretIntegration advokatregisteretIntegration;

    private MockServerRestClientCustomizer customizer;

    @BeforeEach
    public void setUp() {
        when(maskinportenClients.getClient(any())).thenReturn(maskinportenClient);
        when(maskinportenClient.getAccessToken(anyString(), anyList())).thenReturn(mock(AccessToken.class));
        customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        advokatregisteretIntegration = new AdvokatregisteretIntegration(advokatregisteretProperties, maskinportenClients, builder.build());
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
        AuthoritativeSourceException e = assertThrows(
                AuthoritativeSourceException.class,
                () -> advokatregisteretIntegration.retrieve(personIdentifier));
        assertAll(
                () -> assertEquals("server_error", e.getErrorCode()),
                () -> assertEquals("Failed to get information from Advokatregisteret", e.getErrorDescription())
        );
    }

}
