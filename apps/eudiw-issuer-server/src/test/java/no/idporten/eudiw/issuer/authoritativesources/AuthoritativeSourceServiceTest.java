package no.idporten.eudiw.issuer.authoritativesources;

import no.idporten.eudiw.issuer.TestData;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;
import no.idporten.eudiw.issuer.config.AuthoritativeSourceProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class AuthoritativeSourceServiceTest {

    private AuthoritativeSourceService authoritativeSourceService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        CredentialIssuerServerProperties credentialIssuerServerProperties = new CredentialIssuerServerProperties();
        credentialIssuerServerProperties.getAuthoritativeSources().put("junit",
                new AuthoritativeSourceProperties(URI.create("https://junit.eidas2sandkasse.net/connector/junit"),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(3),
                        "X-API-KEY",
                        "junit-key"));
        AuthoritativeSourceService authoritativeSourceService = new AuthoritativeSourceService(credentialIssuerServerProperties);
        MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        mockServer = customizer.getServer();
        RestClient restClient = builder.build();
        authoritativeSourceService.addRestClient("junit", restClient);
        this.authoritativeSourceService = authoritativeSourceService;
    }

    @Test
    void testSuccessfullyRetrieveCredentialData() {
        final String personIdentifier = TestData.syntheticPersonIdentifier();
        final String successReponse = """
                {
                  "credential_data": {
                    "birthdate": "1955-07-24",
                    "expiry_date": "2036-04-09",
                    "family_name": "OSTEKAKE",
                    "given_name": "UBESTIKKELIG",
                    "issuing_authority": "DIGITALISERINGSDIREKTORATET",
                    "issuing_country": "NO",
                    "nationalities": [
                      "NO"
                    ],
                    "personal_administrative_number": "%s",
                    "place_of_birth": {
                      "country": "NO"
                    }
                  }
                }""".formatted(personIdentifier);
        mockServer
                .expect(requestTo("https://junit.eidas2sandkasse.net/connector/junit"))
                .andExpect(header("X-API-KEY", "junit-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.subject.identifier").value(personIdentifier))
                .andExpect(jsonPath("$.credential_type").value("junit.x"))
                .andRespond(withSuccess().body(successReponse).contentType(MediaType.APPLICATION_JSON));
        CredentialData credentialData = authoritativeSourceService.retrieveCredentialData("junit", "junit.x", personIdentifier);
        assertAll(
                () -> assertEquals("UBESTIKKELIG", credentialData.claims().get("given_name")),
                () -> assertEquals("OSTEKAKE", credentialData.claims().get("family_name")),
                () -> assertEquals("1955-07-24", credentialData.claims().get("birthdate")),
                () -> assertEquals("DIGITALISERINGSDIREKTORATET", credentialData.claims().get("issuing_authority")),
                () -> assertEquals("NO", credentialData.claims().get("issuing_country")),
                () -> assertEquals(personIdentifier, credentialData.claims().get("personal_administrative_number")),
                () -> assertNotNull(credentialData.claims().get("expiry_date")),
                () -> assertNotNull(credentialData.claims().get("nationalities")),
                () -> assertNotNull(credentialData.claims().get("place_of_birth"))
        );
        mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 500, 502, 503})
    void test4xx5xxResponseGivesCredentialRequestDeniedException(int status) {
        final String personIdentifier = TestData.syntheticPersonIdentifier();
        final String errorResponse = """
                {
                    "error": "invalid_request_or_something_bad",
                    "error_description": "Something failed (trace_id=00000000000000000000000000000000)"
                }""";
        mockServer
                .expect(requestTo("https://junit.eidas2sandkasse.net/connector/junit"))
                .andRespond(withRawStatus(status).body(errorResponse).contentType(MediaType.APPLICATION_JSON));
        CredentialRequestDeniedException e = assertThrows(
                CredentialRequestDeniedException.class,
                () -> authoritativeSourceService.retrieveCredentialData("junit", "junit.x", personIdentifier));
        assertAll(
                () -> assertEquals("credential_request_denied", e.getError()),
                () -> assertEquals("Failed to get information from authoritative source junit", e.getMessage())
        );
        mockServer.verify();
    }

    @Test
    void test404ResponseGivesCredentialRequestDeniedException() {
        final String personIdentifier = TestData.syntheticPersonIdentifier();
        final String errorResponse = """
                {
                    "error": "credential_data_not_found",
                    "error_description": "Data not found"
                }""";
        mockServer
                .expect(requestTo("https://junit.eidas2sandkasse.net/connector/junit"))
                .andRespond(withResourceNotFound().body(errorResponse).contentType(MediaType.APPLICATION_JSON));
        CredentialRequestDeniedException e = assertThrows(
                CredentialRequestDeniedException.class,
                () -> authoritativeSourceService.retrieveCredentialData("junit", "junit.x", personIdentifier));
        assertAll(
                () -> assertEquals("credential_request_denied", e.getError()),
                () -> assertEquals("No credential data available from authoritative source junit", e.getMessage())
        );
        mockServer.verify();
    }

}
