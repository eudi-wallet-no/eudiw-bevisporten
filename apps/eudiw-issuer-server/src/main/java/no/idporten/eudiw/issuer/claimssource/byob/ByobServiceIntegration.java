package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfigurations;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

import static no.idporten.eudiw.issuer.claimssource.AuthoritativeSource.BYOB;

/**
 * Integration with byob-service to retrieve dynamic credentials configurations (BYOB = Bring Your Own Bevis).
 */
@Service
public class ByobServiceIntegration {

    private static final Logger log = LoggerFactory.getLogger(ByobServiceIntegration.class);
    public static final String API_KEY = "X-API-KEY";

    private final ByobServiceProperties byobServiceProperties;
    private final RestClient byobServiceRestClient;

    @Autowired
    public ByobServiceIntegration(ByobServiceProperties byobServiceProperties,
                                  @Qualifier("byobServiceRestClient") RestClient byobServiceRestClient) {
        this.byobServiceProperties = byobServiceProperties;
        this.byobServiceRestClient = byobServiceRestClient;
    }


    public DynamicCredentialConfigurations retrieveAll() {
        try {
            DynamicCredentialConfigurations credentialConfigurations = byobServiceRestClient
                    .get()
                    .uri("v1/credential-configurations")
                    .header(API_KEY, byobServiceProperties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(DynamicCredentialConfigurations.class);

            // TODO handle byob-service unavailability more gracefully, must be allowed null on startup
            // valider response er gyldig (rett prefix på cred-config-id, osb)
            if (credentialConfigurations == null || credentialConfigurations.getCredentialConfigurations() == null || credentialConfigurations.getCredentialConfigurations().isEmpty()) {
                log.warn("No dynamic credential configurations retrieved from BYOB service, using hardcoded configurations");
//                return getMockedByobResponse();
                return new DynamicCredentialConfigurations(Collections.emptyList());
            }
            return credentialConfigurations;
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(BYOB.name(), "IO error when calling Byob-service", e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException(BYOB.name(), "server_error", "Failed to get information from Byob-service", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
    public DynamicCredentialConfiguration retrieve(String vct) {
        try {
            return byobServiceRestClient
                    .get()
                    .uri("v1/credential-configurations/{vct}", vct)
                    .header(API_KEY, byobServiceProperties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(DynamicCredentialConfiguration.class);
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(BYOB.name(), "IO error when calling Byob-service for vct=%s".formatted(vct), e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException(BYOB.name(), "server_error", "Failed to get information from Byob-service for vct=%s".formatted(vct), HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data from authoritative source. Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new ClaimsSourceException(BYOB.name(), "server_error", "Failed to get information from Byob-service", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

}
