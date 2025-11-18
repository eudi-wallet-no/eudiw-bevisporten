package no.idporten.eudiw.issuer.claimssource.krr;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.claimssource.krr.model.PersonKrr;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.*;
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

    @MockitoBean(name="krrMaskinportenClient")
    private MaskinportenClient maskinportenClient;

    @MockitoBean
    private MockServerRestClientCustomizer customizer;

    @BeforeEach
    public void setUp() {
        when(maskinportenClient.getAccessToken(anyString(), anyList())).thenReturn(mock(AccessToken.class));
        customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        krrIntegration = spy(new KrrIntegration(krrProperties, maskinportenClient, builder.build()));
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
    @DisplayName("When personKrr is null, throws correct exception")
    void testNullPersonKrrThrowsCorrectException() {
        doReturn(null).when(krrIntegration).getPersonKrr(eq("12345678901"));
        assertThrows(ClaimsSourceDataNotFoundException.class, () -> krrIntegration.retrieve("12345678901"));
    }

    @Test
    @DisplayName("When personKrr contact information is outdated, throws correct exception")
    void testPersonKrrThrowsCorrectExceptionWhenOutdatedContactInformation() {

        final String response = """
                 {
                        "personidentifikator": "16903349845", 
                         "reservasjon": "NEI", 
                         "status": "AKTIV",
                         "varslingsstatus": "KAN_IKKE_VARSLES",
                        "kontaktinformasjon":
                        {
                            "mobiltelefonnummer": "48995855",
                            "epostadresse": "nullstilt@default.digdir.no"
                        }
                    }""";

        String personIdentifier = "16903349845";
        customizer.getServer()
                .expect(requestToUriTemplate("rest/v2/person", personIdentifier))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThrows(ClaimsSourceInvalidDataException.class, () -> krrIntegration.retrieve("16903349845"));
    }

    @Test
    @DisplayName("When personKrr is reserved, throws correct exception")
    void testPersonKrrThrowsCorrectExceptionWhenReserved() {

        final String response = """
                 {
                        "personidentifikator": "16903349845", 
                         "reservasjon": "JA", 
                         "status": "AKTIV",
                         "varslingsstatus": "KAN_VARSLES",
                        "kontaktinformasjon":
                        {
                            "mobiltelefonnummer": "48995855",
                            "epostadresse": "nullstilt@default.digdir.no"
                        }
                    }""";

        String personIdentifier = "16903349845";
        customizer.getServer()
                .expect(requestToUriTemplate("rest/v2/person", personIdentifier))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThrows(ClaimsSourceInvalidDataException.class, () -> krrIntegration.retrieve("16903349845"));
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

    @Test
    @DisplayName("When personKrr has not registered telephone and email")
    void testPersonKrrThrowsCorrectExceptionWhenNotTelephoneNumberOrEmailIsRegistered() {

        final String response = """
                 {
                        "personidentifikator": "16903349845", 
                         "reservasjon": "JA", 
                         "status": "AKTIV",
                         "varslingsstatus": "KAN_VARSLES",
                        "kontaktinformasjon":
                        {
                            "mobiltelefonnummer": "",
                            "epostadresse": ""
                        }
                    }""";

        String personIdentifier = "16903349845";
        customizer.getServer()
                .expect(requestToUriTemplate("rest/v2/person", personIdentifier))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThrows(ClaimsSourceInvalidDataException.class, () -> krrIntegration.retrieve("16903349845"));
    }
}
