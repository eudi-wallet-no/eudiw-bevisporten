package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.exception.VerifierServiceException;
import no.idporten.eudiw.bevisgenerator.exception.VerifierServiceIOException;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.config.VerificationProperties;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationResult;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationStartResponse;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;

@Service
public class VerifierServiceImpl implements VerifierService {
    private final RestClient restClient;
    private final VerificationProperties verificationProperties;

    public VerifierServiceImpl(@Qualifier("verificationServiceRestClient") RestClient restClient, VerificationProperties verificationProperties) {
        this.restClient = restClient;
        this.verificationProperties = verificationProperties;
    }

    @Override
    public VerificationTransactionData startVerification(String dcql) {
        VerificationStartResponse response = getVerificationStartResponse(dcql);

        if (response == null) {
            throw new VerifierServiceException("Verifier service returned null response when starting verification");
        }

        URI statusUri = URI.create(verificationProperties.baseUrl() +
                verificationProperties.verificationStatusEndpoint()
                        .replace("{client_application_id}", verificationProperties.clientApplicationId())
                        .replace("{verifier_transaction_id}", response.verifierTransactionId()));
        URI responseUri = URI.create(verificationProperties.baseUrl() +
                verificationProperties.verificationResultEndpoint()
                        .replace("{client_application_id}", verificationProperties.clientApplicationId())
                        .replace("{verifier_transaction_id}", response.verifierTransactionId()));

        return new VerificationTransactionData(response, statusUri, responseUri);

    }

    @Override
    public VerificationResult retrieveVerificationResult(String transactionId) {
        VerificationResult result;
        try {
            result = restClient
                    .get()
                    .uri(verificationProperties.verificationResultEndpoint(), verificationProperties.clientApplicationId(), transactionId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(verifierErrorHandler())
                    .body(VerificationResult.class);
        } catch (ResourceAccessException e) {
            throw new VerifierServiceIOException("IO error when calling Verifier service to retrieve verification result", e);
        } catch (RestClientException e) {
            throw new VerifierServiceException("Configuration error against Verifier-service? path=" + verificationProperties.verificationResultEndpoint(), e);
        }

        if (result == null) {
            throw new VerifierServiceException("Verification result returned null");
        }

        return result;
    }

    private VerificationStartResponse getVerificationStartResponse(String dcql) {
        String apiRequest = buildApiRequest(dcql);

        try {
            return restClient
                    .post()
                    .uri(verificationProperties.verificationStartEndpoint(), verificationProperties.clientApplicationId())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(apiRequest)
                    .retrieve()
                    .onStatus(verifierErrorHandler())
                    .body(VerificationStartResponse.class);
        } catch (ResourceAccessException e) {
            throw new VerifierServiceIOException("IO error when calling Verifier service to start verification", e);
        } catch (RestClientException e) {
            throw new VerifierServiceException("Configuration error against Verifier-service? path=" + verificationProperties.verificationStartEndpoint(), e);
        }
    }

    private String buildApiRequest(String dcql) {
        return """
                {
                  "dcql_query": %s
                }""".formatted(dcql);
    }

    private static ResponseErrorHandler verifierErrorHandler() {
        return new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().isError();
            }

            @Override
            public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
                HttpStatusCode statusCode = response.getStatusCode();
                HttpStatus status = HttpStatus.resolve(statusCode.value());

                if (status != null && status.is4xxClientError()) {
                    throw new VerifierServiceException("Verifier service returned 4xx error", status);
                }
                if (status != null && status.is5xxServerError()) {
                    throw new VerifierServiceException("Verifier service returned 5xx error", status);
                }
                throw new VerifierServiceException("Verifier service returned error status " + statusCode.value());
            }
        };
    }
}
