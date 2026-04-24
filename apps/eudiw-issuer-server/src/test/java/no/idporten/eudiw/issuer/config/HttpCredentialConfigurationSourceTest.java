package no.idporten.eudiw.issuer.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("When retrieving dynamic credential configurations from the Byob-service using HttpCredentialConfigurationSource")
public class HttpCredentialConfigurationSourceTest {

    private HttpCredentialConfigurationSource credentialConfigurationSource;
    private MockRestServiceServer mockServer;

    private static final URI EXTERNAL_SERVICE_URL = URI.create("http://my-byob-service-test:8/v1/admin/credential-configurations");

    private static final String EXAMPLE_JSON_RESPONSE = """
            {
               "credential_configurations": [
                 {
                   "credential_configuration_id": "net.eidas2sandkasse:programmer_stats_sd_jwt_vc",
                   "credential_metadata": {
                     "display": [
                       {
                         "name": "Utvikler-stats",
                         "locale": "no",
                         "background_color": null,
                         "text_color": null
                       }
                     ],
                     "claims": [
                       {
                         "path": "preferred_language",
                         "type": "string",
                         "mime_type": null,
                         "mandatory": true,
                         "display": [
                           {
                             "name": "Foretrukken språk",
                             "locale": "no",
                             "background_color": null,
                             "text_color": null
                           }
                         ]
                       },
                       {
                         "path": "lines_per_minute",
                         "type": "string",
                         "mime_type": null,
                         "mandatory": true,
                         "display": [
                           {
                             "name": "Linier kode pr. minut",
                             "locale": "no",
                             "background_color": null,
                             "text_color": null
                           }
                         ]
                       }
                     ]
                   },
                   "credential_type": "net.eidas2sandkasse:programmer_stats",
                   "example_credential_data": {
                     "lines_per_minute": "42",
                     "preferred_language": "Oz"
                   },
                   "format": "dc+sd-jwt",
                   "scope": "eudiw:eidas2sandkasse:dynamicvc"
                 }
               ]
             }""";

    @BeforeEach
    public void setUp() {
        APIConnectionProperties apiConnectionProperties = new APIConnectionProperties(EXTERNAL_SERVICE_URL, Duration.ofSeconds(3), Duration.ofSeconds(3), null, null);
        MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = apiConnectionProperties.createRestClientBuilder();
        customizer.customize(builder);
        mockServer = customizer.getServer();
        RestClient restClient = builder.build();
        credentialConfigurationSource = new HttpCredentialConfigurationSource(apiConnectionProperties);
        credentialConfigurationSource.setRestClient(restClient);
    }

    @DisplayName("then return all configurations from success response")
    @Test
    void testRetrieveAllSuccess() throws JsonProcessingException {
        mockServer.expect(requestTo(EXTERNAL_SERVICE_URL)).andRespond(withSuccess(EXAMPLE_JSON_RESPONSE, MediaType.APPLICATION_JSON));
        credentialConfigurationSource.init();
        List<ExtendedCredentialConfiguration> retrievedCredentialConfigurations = credentialConfigurationSource.retrieve();
        assertAll(() -> assertNotNull(retrievedCredentialConfigurations),
                () -> assertEquals(1, retrievedCredentialConfigurations.size()),
                () -> assertEquals("net.eidas2sandkasse:programmer_stats_sd_jwt_vc", retrievedCredentialConfigurations.getFirst().getCredentialConfigurationId()),
                () -> assertEquals("net.eidas2sandkasse:programmer_stats", retrievedCredentialConfigurations.getFirst().getCredentialType())
        );
    }

    @DisplayName("then uninitialized will return empty list")
    @Test
    void testRetrieveUninitialized() {
        List<ExtendedCredentialConfiguration> retrievedCredentialConfigurations = credentialConfigurationSource.retrieve();
        assertAll(
                () -> assertNotNull(retrievedCredentialConfigurations),
                () -> assertTrue(retrievedCredentialConfigurations.isEmpty())
        );
    }

    @DisplayName("then do not fail on empty response")
    @Test
    void testRetrieveAllEmptySuccess() {
        mockServer.expect(requestTo(EXTERNAL_SERVICE_URL)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        credentialConfigurationSource.init();
        List<ExtendedCredentialConfiguration> retrievedCredentialConfigurations = credentialConfigurationSource.retrieve();
        assertAll(
                () -> assertNotNull(retrievedCredentialConfigurations),
                () -> assertTrue(retrievedCredentialConfigurations.isEmpty())
        );
        mockServer.verify();
    }

    @DisplayName("then do not overwrite existing configurations when refresh fails")
    @Test
    void testRefreshFails() {
        mockServer.expect(requestTo(EXTERNAL_SERVICE_URL)).andRespond(withSuccess(EXAMPLE_JSON_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(EXTERNAL_SERVICE_URL)).andRespond(withServerError());
        credentialConfigurationSource.init();
        List<ExtendedCredentialConfiguration> credentialConfigurations = credentialConfigurationSource.retrieve();
        assertThrows(CredentialConfigurationSourceException.class, () -> credentialConfigurationSource.refresh());
        mockServer.verify();
        assertEquals(credentialConfigurations, credentialConfigurationSource.retrieve());
    }

}
