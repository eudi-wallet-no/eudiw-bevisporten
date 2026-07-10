package no.idporten.eudiw.login.openid4vp.verifier;

import no.idporten.eudiw.login.openid4vp.verifier.model.StartVerificationRequest;
import no.idporten.eudiw.login.openid4vp.verifier.model.StartVerificationResponse;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationResultResponse;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationStatusResponse;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * Integration with verification service REST API.
 */
@Service
public class VerifierServiceIntegration {

    private final RestClient restClient;
    public static final String VERIFIER_CLIENT_APPLICATION_ID = "idporten-login";

    public VerifierServiceIntegration(VerifierServiceProperties verifierServiceProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(verifierServiceProperties.uri().toString())
                .requestFactory(createRequestFactory(verifierServiceProperties))
                .build();
    }

    /**
     * Start a verification process.
     */
    public StartVerificationResponse startVerification(StartVerificationRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/{clientApplicationId}/verify/start/", VERIFIER_CLIENT_APPLICATION_ID)
                    .body(request)
                    .retrieve()
                    .body(StartVerificationResponse.class);
        } catch (HttpClientErrorException e) {
            throw handleBadRequest(e);
        } catch (HttpServerErrorException e) {
            throw handleServerError(e);
        }
    }

    /**
     * Check a verification process status.
     */
    public VerificationStatusResponse retrieveStatus(String verifierTransactionId) {
        try {
            return restClient.get()
                    .uri("/api/v1/{clientApplicationId}/verify/status/{verifierTransactionId}", VERIFIER_CLIENT_APPLICATION_ID, verifierTransactionId)
                    .retrieve()
                    .body(VerificationStatusResponse.class);
        } catch (HttpClientErrorException e) {
            throw handleBadRequest(e);
        } catch (HttpServerErrorException e) {
            throw handleServerError(e);
        }
    }

    /**
     * Retrieve verification process result.
     */
    public VerificationResultResponse retrieveVerifiedCredentials(String verifierTransactionId) {
        try {
            return restClient.get()
                    .uri("/api/v1/{clientApplicationId}/verify/result/{verifierTransactionId}", VERIFIER_CLIENT_APPLICATION_ID, verifierTransactionId)
                    .retrieve()
                    .body(VerificationResultResponse.class);
        } catch (HttpClientErrorException e) {
            throw handleBadRequest(e);
        } catch (HttpServerErrorException e) {
            throw handleServerError(e);
        }
    }

    private SimpleClientHttpRequestFactory createRequestFactory(VerifierServiceProperties verifierServiceProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(verifierServiceProperties.connectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(verifierServiceProperties.readTimeout().toMillis()));
        return requestFactory;
    }

    private RuntimeException handleBadRequest(HttpClientErrorException e) {
        return new OAuth2Exception(OAuth2Exception.SERVER_ERROR, "Failed to verify credentials", HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }

    private RuntimeException handleServerError(HttpServerErrorException e) {
        return new OAuth2Exception(OAuth2Exception.SERVER_ERROR, "Failed to verify credentials", HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }

}
