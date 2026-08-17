package no.idporten.eudiw.connector.authoritativesources.krr;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import no.idporten.eudiw.connector.authoritativesources.krr.model.PersonKrr;
import no.idporten.lib.maskinporten.client.JwtGrantTokenInterceptor;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import no.idporten.lib.maskinporten.client.MaskinportenClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("When integrating krr")
@ActiveProfiles("junit")
@SpringBootTest
public class KrrIntegrationTest {

    private KrrIntegration krrIntegration;

    @Autowired
    private KrrProperties krrProperties;

    @MockitoBean
    private MaskinportenClients maskinportenClients;

    @MockitoBean
    private MaskinportenClient maskinportenClient;

    @MockitoBean
    private JwtGrantTokenInterceptor jwtGrantTokenInterceptor;

    @MockitoBean
    private MockServerRestClientCustomizer customizer;

    @BeforeEach
    public void setUp() {
        when(maskinportenClients.getClient(anyString())).thenReturn(maskinportenClient);
        when(maskinportenClient.getAccessToken(anyString(), anyList())).thenReturn(mock(AccessToken.class));
        customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        krrIntegration = spy(new KrrIntegration(krrProperties, maskinportenClients, builder.build()));
    }


    @Test
    @DisplayName("When getting correct response from krr, it behaves correctly")
    public void testOkResponseReturnsPersonKrr() {
        final String wantedResponse = """
                {
                        "personidentifikator": "16903349844",
                         "reservasjon": "NEI",
                         "status": "AKTIV",
                         "varslingsstatus": "KAN_VARSLES",
                        "kontaktinformasjon":
                        {
                            "mobiltelefonnummer": "48995855",
                            "epostadresse": "nullstilt@default.digdir.no"
                        }
                }""";
        String personIdentifier = "16903349844";
        customizer.getServer()
                .expect(requestToUriTemplate("rest/v2/person", personIdentifier))
                .andRespond(withSuccess(wantedResponse, MediaType.APPLICATION_JSON));
        PersonKrr personKrr = krrIntegration.retrieve(personIdentifier);
        assertAll(
                () -> assertEquals("16903349844", personKrr.personidentifikator()),
                () -> assertEquals("48995855", personKrr.kontaktinformasjon().mobiltelefonnummer()),
                () -> assertEquals("nullstilt@default.digdir.no", personKrr.kontaktinformasjon().epostadresse())
        );

    }

    @Test
    @DisplayName("When personKrrs phone number is not registered, but email is")
    void testPersonKrrrReturnsOnlyEmailWhenPhoneNotRegistered() {
        final String response = """
                {
                       "personidentifikator": "16903349845",
                        "reservasjon": "NEI",
                        "status": "AKTIV",
                        "varslingsstatus": "KAN_VARSLES",
                       "kontaktinformasjon":
                       {
                           "mobiltelefonnummer": "",
                           "epostadresse": "nullstilt@default.digdir.no"
                       }
                }""";

        String personIdentifier = "16903349845";
        customizer.getServer()
                .expect(requestToUriTemplate("rest/v2/person", personIdentifier))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        PersonKrr personKrr = krrIntegration.retrieve(personIdentifier);
        assertAll(
                () -> assertEquals("", personKrr.kontaktinformasjon().mobiltelefonnummer()),
                () -> assertEquals("nullstilt@default.digdir.no", personKrr.kontaktinformasjon().epostadresse())

        );
    }
}
