package no.idporten.eudiw.issuer.claimssource.byob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfigurations;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceIOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ActiveProfiles("junit")
@RestClientTest(components = {ByobServiceIntegration.class, ByobServiceConfiguration.class, ByobServiceProperties.class})
@AutoConfigureMockMvc
@DisplayName("Integration tests for ByobServiceIntegration")
class ByobServiceIntegrationTest {

    private final ByobServiceProperties byobServiceProperties = new ByobServiceProperties(URI.create("http://my-byob-service-test:8/"), null, Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(10));

    private MockRestServiceServer mockServer;

    private ByobServiceIntegration byobServiceIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        mockServer = customizer.getServer();
        byobServiceIntegration = new ByobServiceIntegration(byobServiceProperties, builder.build());
    }

    @Nested
    @DisplayName("when retrieveAll dynamic credential configurations")
    class RetrieveAll {


        @DisplayName("should return list of dynamic credential configurations when successful")
        @Test
        void testRetrieveAllSuccess() throws JsonProcessingException {
            List<DynamicCredentialConfiguration> dynamicCredentialConfiguration = List.of(getDynamicCredentialConfiguration("sandkasse:1"), getDynamicCredentialConfiguration("sandkasse:2"));
            DynamicCredentialConfigurations dcc = createDynamicCredentialConfigurations(dynamicCredentialConfiguration);
            String bevisDefinisjonarString = objectMapper.writeValueAsString(dcc);

            mockServer.expect(requestTo("v1/credential-configurations"))
                    .andRespond(withSuccess(bevisDefinisjonarString, MediaType.APPLICATION_JSON));

            DynamicCredentialConfigurations dynamicCredentialConfigurations = byobServiceIntegration.retrieveAll();
            assertAll(
                    () -> assertNotNull(dynamicCredentialConfigurations),
                    () -> assertEquals(dcc.getCredentialConfigurations().size(), dynamicCredentialConfigurations.getCredentialConfigurations().size()),
                    () -> assertEquals(dcc.getCredentialConfigurations().getFirst().credentialConfigurationId(), dynamicCredentialConfigurations.getCredentialConfigurations().getFirst().credentialConfigurationId()),
                    () -> assertEquals(dcc.getCredentialConfigurations().getFirst().vct(), dynamicCredentialConfigurations.getCredentialConfigurations().getFirst().vct())
            );

        }

        @DisplayName("should return empty list when no dynamic credential configurations exists")
        @Test
        void testRetrieveAllEmptySuccess() {

            mockServer.expect(requestTo("v1/credential-configurations"))
                    .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

            DynamicCredentialConfigurations dynamicCredentialConfigurations = byobServiceIntegration.retrieveAll();
            assertAll(
                    () -> assertNotNull(dynamicCredentialConfigurations),
                    () -> assertNotNull(dynamicCredentialConfigurations.getCredentialConfigurations()),
                    () -> assertTrue(dynamicCredentialConfigurations.getCredentialConfigurations().isEmpty())
            );
        }

        @DisplayName("should throw ClaimsSourceException when Byob-service returns 500")
        @Test
        void testRetrieveAllFails500() {

            mockServer.expect(requestTo("v1/credential-configurations"))
                    .andRespond(withServerError());
            assertThrows(ClaimsSourceException.class, () -> byobServiceIntegration.retrieveAll());
        }

        @DisplayName("should throw ClaimsSourceIOException when Byob-service is unreachable")
        @Test
        void testRetrieveAllFailsIOError() {

            mockServer.expect(requestTo("v1/credential-configurations"))
                    .andRespond(withException(new IOException("can not connect to byob-service")));
            assertThrows(ClaimsSourceIOException.class, () -> byobServiceIntegration.retrieveAll());
        }
    }

    @Nested
    @DisplayName("when retrieve 1 by VCT")
    class RetriveByVct {

        @DisplayName("should return dynamic credential configuration when successful")
        @Test
        void testRetrieveVctOneSuccess() throws JsonProcessingException {
            String vct = "mitt_bevis";
            DynamicCredentialConfiguration dcc = getDynamicCredentialConfiguration(vct);
            String bevisDefinisjonarString = objectMapper.writeValueAsString(dcc);

            mockServer.expect(requestTo("v1/credential-configuration/%s".formatted(vct)))
                    .andRespond(withSuccess(bevisDefinisjonarString, MediaType.APPLICATION_JSON));

            DynamicCredentialConfiguration dynamicCredentialConfiguration = byobServiceIntegration.retrieve(vct);
            assertAll(
                    () -> assertNotNull(dynamicCredentialConfiguration),
                    () -> assertEquals(dcc.credentialConfigurationId(), dynamicCredentialConfiguration.credentialConfigurationId()),
                    () -> assertEquals(dcc.vct(), dynamicCredentialConfiguration.vct())
            );
        }

        @DisplayName("should throw ClaimsSourceException when Byob-service returns 404 Not Found")
        @Test
        void testRetrieveVctNotFound() {
            String vct = "mitt_bevis";

            mockServer.expect(requestTo("v1/credential-configuration/%s".formatted(vct)))
                    .andRespond(withResourceNotFound());

            assertThrows(ClaimsSourceException.class, () -> byobServiceIntegration.retrieve(vct));
        }

        @DisplayName("should return null when input vct is null")
        @Test
        void testRetrieveVctWithNullVctReturnNull() {
            String vct = null;

            assertNull(byobServiceIntegration.retrieve(vct));
        }
    }

    @Nested
    @DisplayName("when retrieve 1 by credential_configuration_id")
    class RetriveByCredentialConfigurationId {

        @DisplayName("should return dynamic credential configuration when successful")
        @Test
        void testRetrieveSearchOneSuccess() throws JsonProcessingException {
            String vct = "mitt_bevis";
            String credentialConfigurationId = vct + "_sd_jwt";
            DynamicCredentialConfiguration dcc = getDynamicCredentialConfiguration(vct);
            String bevisDefinisjonarString = objectMapper.writeValueAsString(dcc);

            mockServer.expect(requestTo("v1/credential-configuration/search?credentialConfigurationId=%s".formatted(credentialConfigurationId)))
                    .andRespond(withSuccess(bevisDefinisjonarString, MediaType.APPLICATION_JSON));

            DynamicCredentialConfiguration dynamicCredentialConfiguration = byobServiceIntegration.searchByCredentialConfigurationId(credentialConfigurationId);
            assertAll(
                    () -> assertNotNull(dynamicCredentialConfiguration),
                    () -> assertEquals(dcc.credentialConfigurationId(), dynamicCredentialConfiguration.credentialConfigurationId()),
                    () -> assertEquals(dcc.vct(), dynamicCredentialConfiguration.vct())
            );
        }

        @DisplayName("should throw ClaimsSourceException when Byob-service returns 404 Not Found")
        @Test
        void testRetrieveSearchNotFound() {
            String vct = "mitt_bevis";
            String credentialConfigurationId = vct + "_sd_jwt";
            mockServer.expect(requestTo("v1/credential-configuration/search?credentialConfigurationId=%s".formatted(credentialConfigurationId)))
                    .andRespond(withResourceNotFound());

            assertThrows(ClaimsSourceException.class, () -> byobServiceIntegration.searchByCredentialConfigurationId(credentialConfigurationId));
        }

        @DisplayName("should return null when input credential_configuration_id is null")
        @Test
        void testRetrieveSearchWithNullVctReturnNull() {
            String credentialConfigurationId = null;

            assertNull(byobServiceIntegration.searchByCredentialConfigurationId(credentialConfigurationId));
        }
    }

    @Nested
    @DisplayName("when retrieveAll is cached")
    class RetrieveAllByCache {

        @Test
        @DisplayName("should return cached dynamic credential configurations when cache is valid")
        void testRetrieveAllFromCache() throws JsonProcessingException {
            List<DynamicCredentialConfiguration> dynamicCredentialConfiguration = List.of(getDynamicCredentialConfiguration("sandkasse:1"), getDynamicCredentialConfiguration("sandkasse:2"));
            DynamicCredentialConfigurations dcc = createDynamicCredentialConfigurations(dynamicCredentialConfiguration);
            String bevisDefinisjonarString = objectMapper.writeValueAsString(dcc);
            // First call to populate cache
            mockServer.expect(ExpectedCount.once(), requestTo("v1/credential-configurations"))
                    .andRespond(withSuccess(bevisDefinisjonarString, MediaType.APPLICATION_JSON));
            DynamicCredentialConfigurations dynamicCredentialConfigurations = byobServiceIntegration.retrieveAll();
            assertAll(
                    () -> assertNotNull(dynamicCredentialConfigurations),
                    () -> assertEquals(dcc.getCredentialConfigurations().size(), dynamicCredentialConfigurations.getCredentialConfigurations().size())
            );
            // Second call should hit cache, so no new mock expectation is set
            DynamicCredentialConfigurations cachedDynamicCredentialConfigurations = byobServiceIntegration.retrieveAll();
            assertAll(
                    () -> assertNotNull(cachedDynamicCredentialConfigurations),
                    () -> assertEquals(dcc.getCredentialConfigurations().size(), cachedDynamicCredentialConfigurations.getCredentialConfigurations().size())
            );


        }
    }

    private DynamicCredentialConfigurations createDynamicCredentialConfigurations(List<DynamicCredentialConfiguration> dynamicCredentialConfiguration) {
        return DynamicCredentialConfigurations.builder()
                .credentialConfigurations(dynamicCredentialConfiguration)
                .build();
    }

    private static DynamicCredentialConfiguration getDynamicCredentialConfiguration(final String vct) {
        return DynamicCredentialConfiguration.builder().credentialConfigurationId(vct + "_sd_jwt").vct(vct).build();
    }
}