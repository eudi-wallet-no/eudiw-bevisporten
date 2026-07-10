package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.exception.VerifierServiceException;
import no.idporten.eudiw.bevisgenerator.exception.VerifierServiceIOException;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.config.VerificationProperties;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationResult;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationStartResponse;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VerifierServiceTest {

    private RestClient restClient;
    private VerificationProperties verificationProperties;
    private VerifierService verifierService;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        verificationProperties = new VerificationProperties(
                URI.create("http://verifier"),
                "/verifier/{client_application_id}/start",
                "/verifier/{client_application_id}/status/{verifier_transaction_id}",
                "/verifier/{client_application_id}/result/{verifier_transaction_id}",
                "client-123"
        );
        verifierService = new VerifierServiceImpl(restClient, verificationProperties);

        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class, RETURNS_SELF);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class, RETURNS_SELF);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/verifier/{client_application_id}/start", "client-123")).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(ResponseErrorHandler.class))).thenReturn(responseSpec);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/verifier/{client_application_id}/result/{verifier_transaction_id}", "client-123", "tx-id"))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void startVerificationReturnsTransactionDataWithResolvedUris() {
        VerificationStartResponse startResponse = new VerificationStartResponse(
                "eudi-openid4vp://example",
                "data:image/png;base64,abc123",
                "tx-id"
        );
        when(responseSpec.body(VerificationStartResponse.class)).thenReturn(startResponse);

        VerificationTransactionData result = verifierService.startVerification("{\"credentials\":[]}");

        assertNotNull(result);
        assertEquals("tx-id", result.verificationStartResponse().verifierTransactionId());
        assertEquals(
                URI.create("http://verifier/verifier/client-123/status/tx-id"),
                result.statusUri()
        );
        assertEquals(
                URI.create("http://verifier/verifier/client-123/result/tx-id"),
                result.resultUri()
        );
    }

    @Test
    void startVerificationThrowsVerifierServiceExceptionWhenResponseIsNull() {
        when(responseSpec.body(VerificationStartResponse.class)).thenReturn(null);

        VerifierServiceException exception = assertThrows(
                VerifierServiceException.class,
                () -> verifierService.startVerification("{\"credentials\":[]}")
        );

        assertEquals("Verifier service returned null response when starting verification", exception.getMessage());
    }

    @Test
    void startVerificationWrapsResourceAccessExceptionAsVerifierServiceIOException() {
        doThrow(new ResourceAccessException("boom"))
                .when(responseSpec).body(VerificationStartResponse.class);

        VerifierServiceIOException exception = assertThrows(
                VerifierServiceIOException.class,
                () -> verifierService.startVerification("{\"credentials\":[]}")
        );

        assertEquals("IO error when calling Verifier service to start verification", exception.getMessage());
        assertInstanceOf(ResourceAccessException.class, exception.getCause());
    }

    @Test
    void retrieveVerificationResultReturnsResponseBody() {
        VerificationResult expected = new VerificationResult(
                "tx-id",
                Map.of("proof_of_age", List.of(new VerificationResult.CredentialPresentation(Map.of("age_over_18", true))))
        );
        when(responseSpec.body(VerificationResult.class)).thenReturn(expected);

        VerificationResult result = verifierService.retrieveVerificationResult("tx-id");

        assertEquals(expected, result);
    }

    @Test
    void retrieveVerificationResultWrapsResourceAccessExceptionAsVerifierServiceIOException() {
        doThrow(new ResourceAccessException("boom"))
                .when(responseSpec).body(VerificationResult.class);

        VerifierServiceIOException exception = assertThrows(
                VerifierServiceIOException.class,
                () -> verifierService.retrieveVerificationResult("tx-id")
        );

        assertEquals("IO error when calling Verifier service to retrieve verification result", exception.getMessage());
        assertInstanceOf(ResourceAccessException.class, exception.getCause());
    }

    @Test
    void retrieveVerificationResultWrapsRestClientExceptionAsVerifierServiceException() {
        doThrow(new RestClientException("bad config"))
                .when(responseSpec).body(VerificationResult.class);

        VerifierServiceException exception = assertThrows(
                VerifierServiceException.class,
                () -> verifierService.retrieveVerificationResult("tx-id")
        );

        assertEquals(
                "Configuration error against Verifier-service? path=/verifier/{client_application_id}/result/{verifier_transaction_id}",
                exception.getMessage()
        );
        assertInstanceOf(RestClientException.class, exception.getCause());
    }

    @Test
    void retrieveVerificationResultThrowsVerifierServiceExceptionWhenResponseIsNull() {
        when(responseSpec.body(VerificationResult.class)).thenReturn(null);

        VerifierServiceException exception = assertThrows(
                VerifierServiceException.class,
                () -> verifierService.retrieveVerificationResult("tx-id")
        );

        assertEquals("Verification result returned null", exception.getMessage());
    }
}
