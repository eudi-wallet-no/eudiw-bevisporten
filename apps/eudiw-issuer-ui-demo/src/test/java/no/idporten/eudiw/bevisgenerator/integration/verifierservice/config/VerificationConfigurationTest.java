package no.idporten.eudiw.bevisgenerator.integration.verifierservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

@DisplayName("When configuring the verifier-service RestClient")
class VerificationConfigurationTest {

    private static final String BASE_URL = "http://verifier";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String API_KEY = "api-key-abc";

    @Test
    @DisplayName("When an API key is configured, then requests include the X-API-KEY header")
    void sendsConfiguredApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        VerificationProperties properties = new VerificationProperties(
                URI.create(BASE_URL),
                null,
                null,
                null,
                null,
                API_KEY
        );
        RestClient restClient = new VerificationConfiguration().restClient(builder, properties);

        mockServer.expect(requestTo(BASE_URL + "/verification"))
                .andExpect(method(GET))
                .andExpect(header(API_KEY_HEADER, API_KEY))
                .andRespond(withNoContent());

        restClient.get()
                .uri("/verification")
                .retrieve()
                .toBodilessEntity();

        mockServer.verify();
    }
}
