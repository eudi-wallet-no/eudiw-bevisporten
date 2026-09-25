package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.exception.VerifierServiceException;
import no.idporten.eudiw.bevisgenerator.exception.VerifierServiceIOException;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.config.VerificationProperties;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationResult;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerifiedCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("When using the verifierService")
class VerifierServiceTest {

    private static final String BASE_URL = "http://verifier";
    private static final String CLIENT_APPLICATION_ID = "client-123";
    private static final String TRANSACTION_ID = "tx-id";
    private static final String API_KEY = "api-key-abc";
    private static final String REQUEST_BODY = "{\"credentials\":[]}";
    private static final String START_ENDPOINT = "/verifier/{client_application_id}/start";
    private static final String STATUS_ENDPOINT = "/verifier/{client_application_id}/status/{verifier_transaction_id}";
    private static final String RESULT_ENDPOINT = "/verifier/{client_application_id}/result/{verifier_transaction_id}";
    private static final String START_URL = BASE_URL + "/verifier/" + CLIENT_APPLICATION_ID + "/start";
    private static final String RESULT_URL = BASE_URL + "/verifier/" + CLIENT_APPLICATION_ID + "/result/" + TRANSACTION_ID;

    private MockRestServiceServer mockServer;
    private VerifierService verifierService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        VerificationProperties verificationProperties = new VerificationProperties(
                URI.create(BASE_URL),
                START_ENDPOINT,
                STATUS_ENDPOINT,
                RESULT_ENDPOINT,
                CLIENT_APPLICATION_ID,
                API_KEY
        );
        verifierService = new VerifierServiceImpl(builder.build(), verificationProperties);
    }

    @Test
    @DisplayName("When starting verification, then transaction data with resolved URIs is returned")
    void startVerificationReturnsTransactionDataWithResolvedUris() {
        mockServer.expect(requestTo(START_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(REQUEST_BODY))
                .andRespond(withSuccess("""
                        {
                          "authorization_request": "eudi-openid4vp://example",
                          "authorization_request_qr_code": "data:image/png;base64,abc123",
                          "verifier_transaction_id": "tx-id"
                        }
                        """, MediaType.APPLICATION_JSON));

        VerificationTransactionData result = verifierService.startVerification(REQUEST_BODY);

        assertNotNull(result);
        assertEquals(TRANSACTION_ID, result.verificationStartResponse().verifierTransactionId());
        assertEquals(
                URI.create(BASE_URL + "/verifier/" + CLIENT_APPLICATION_ID + "/start"),
                result.requestUri()
        );
        assertEquals(REQUEST_BODY, result.requestBody());
        assertEquals(
                URI.create(BASE_URL + "/verifier/" + CLIENT_APPLICATION_ID + "/status/" + TRANSACTION_ID),
                result.statusUri()
        );
        assertEquals(
                URI.create(BASE_URL + "/verifier/" + CLIENT_APPLICATION_ID + "/result/" + TRANSACTION_ID),
                result.resultUri()
        );
        mockServer.verify();
    }

    @Test
    @DisplayName("When starting verification returns no content, then a verifier service exception is thrown")
    void startVerificationThrowsVerifierServiceExceptionWhenResponseIsNull() {
        mockServer.expect(requestTo(START_URL))
                .andRespond(withNoContent());

        VerifierServiceException exception = assertThrows(
                VerifierServiceException.class,
                () -> verifierService.startVerification(REQUEST_BODY)
        );

        assertEquals("Verifier service returned null response when starting verification", exception.getMessage());
        mockServer.verify();
    }

    @Test
    @DisplayName("When starting verification cannot reach the verifier service, then an IO exception is thrown")
    void startVerificationWrapsResourceAccessExceptionAsVerifierServiceIOException() {
        mockServer.expect(requestTo(START_URL))
                .andRespond(withException(new ConnectException("boom")));

        VerifierServiceIOException exception = assertThrows(
                VerifierServiceIOException.class,
                () -> verifierService.startVerification(REQUEST_BODY)
        );

        assertEquals("IO error when calling Verifier service to start verification", exception.getMessage());
        assertInstanceOf(ResourceAccessException.class, exception.getCause());
        mockServer.verify();
    }

    @Test
    @DisplayName("When retrieving a verification result, then the response body is returned")
    void retrieveVerificationResultReturnsResponseBody() {
        mockServer.expect(requestTo(RESULT_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                          "verifier_transaction_id": "tx-id",
                          "credentials": {
                            "proof_of_age": [
                              {
                                "claims": {
                                  "age_over_18": true
                                },
                                "valid": true,
                                "validation_details": []
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        VerificationResult expected = new VerificationResult(
                TRANSACTION_ID,
                Map.of("proof_of_age", List.of(new VerifiedCredential(Map.of("age_over_18", true), true, List.of())))
        );

        VerificationResult result = verifierService.retrieveVerificationResult(TRANSACTION_ID);

        assertEquals(expected, result);
        mockServer.verify();
    }

    @Test
    @DisplayName("When retrieving a verification result cannot reach the verifier service, then an IO exception is thrown")
    void retrieveVerificationResultWrapsResourceAccessExceptionAsVerifierServiceIOException() {
        mockServer.expect(requestTo(RESULT_URL))
                .andRespond(withException(new ConnectException("boom")));

        VerifierServiceIOException exception = assertThrows(
                VerifierServiceIOException.class,
                () -> verifierService.retrieveVerificationResult(TRANSACTION_ID)
        );

        assertEquals("IO error when calling Verifier service to retrieve verification result", exception.getMessage());
        assertInstanceOf(ResourceAccessException.class, exception.getCause());
        mockServer.verify();
    }

    @Test
    @DisplayName("When retrieving a malformed verification result, then a verifier service exception is thrown")
    void retrieveVerificationResultWrapsRestClientExceptionAsVerifierServiceException() {
        mockServer.expect(requestTo(RESULT_URL))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

        VerifierServiceException exception = assertThrows(
                VerifierServiceException.class,
                () -> verifierService.retrieveVerificationResult(TRANSACTION_ID)
        );

        assertEquals(
                "Configuration error against Verifier-service? path=" + RESULT_ENDPOINT,
                exception.getMessage()
        );
        assertInstanceOf(RestClientException.class, exception.getCause());
        mockServer.verify();
    }

    @Test
    @DisplayName("When retrieving a verification result returns no content, then a verifier service exception is thrown")
    void retrieveVerificationResultThrowsVerifierServiceExceptionWhenResponseIsNull() {
        mockServer.expect(requestTo(RESULT_URL))
                .andRespond(withNoContent());

        VerifierServiceException exception = assertThrows(
                VerifierServiceException.class,
                () -> verifierService.retrieveVerificationResult(TRANSACTION_ID)
        );

        assertEquals("Verification result returned null", exception.getMessage());
        mockServer.verify();
    }
}
