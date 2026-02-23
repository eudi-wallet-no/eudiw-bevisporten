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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.BYOB;

/**
 * Integration with byob-service to retrieve dynamic credentials configurations (BYOB = Bring Your Own Bevis).
 */
@Service
public class ByobServiceIntegration {

    private static final Logger log = LoggerFactory.getLogger(ByobServiceIntegration.class);
    public static final String API_KEY = "X-API-KEY";

    private final ByobServiceProperties byobServiceProperties;
    private final RestClient byobServiceRestClient;

    private volatile DynamicCredentialConfigurations cachedCCs = null;
    private volatile LocalDateTime lastUpdated;
    private static final Object lock = new Object();

    @Autowired
    public ByobServiceIntegration(ByobServiceProperties byobServiceProperties,
                                  @Qualifier("byobServiceRestClient") RestClient byobServiceRestClient) {
        this.byobServiceProperties = byobServiceProperties;
        this.byobServiceRestClient = byobServiceRestClient;
    }


    public DynamicCredentialConfigurations retrieveAll() {

        if (cachedCCs == null || isCacheExpired()) {
            synchronized (lock) {
                // Double-check locking because of wait time between first check and acquiring the lock
                if (cachedCCs == null || isCacheExpired()) {
                    cachedCCs = retrieveAllFresh();
                    lastUpdated = LocalDateTime.now();
                }
            }
        }
        return cachedCCs;
    }

    private boolean isCacheExpired() {
        if (lastUpdated == null) {
            // Cache has never been updated; treat as expired
            return true;
        }
        Duration cacheDuration = byobServiceProperties.cacheTtl();
        return lastUpdated.plusSeconds(cacheDuration.getSeconds()).isBefore(LocalDateTime.now());
    }

    private DynamicCredentialConfigurations retrieveAllFresh() {
        try {
            DynamicCredentialConfigurations credentialConfigurations = byobServiceRestClient
                    .get()
                    .uri("v1/admin/credential-configurations")
                    .header(API_KEY, byobServiceProperties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(DynamicCredentialConfigurations.class);

            // valider response er gyldig (rett prefix på cred-config-id, osb)
            if (credentialConfigurations == null || credentialConfigurations.getCredentialConfigurations() == null || credentialConfigurations.getCredentialConfigurations().isEmpty()) {
                log.warn("No dynamic credential configurations retrieved from BYOB service, using hardcoded configurations");
                return new DynamicCredentialConfigurations(Collections.emptyList());
            }
            log.info("Retrieved all credential-configurations from byob-service, count={}", credentialConfigurations.getCredentialConfigurations().size());
            return credentialConfigurations;
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(BYOB.name(), "IO error when calling Byob-service", e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException(BYOB.name(), "server_error", "Failed to get information from Byob-service", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public DynamicCredentialConfiguration retrieve(String credentialType) {
        if (credentialType == null) {
            log.warn("credentialType is null");
            return null;
        }
        if (cachedCCs != null && cachedCCs.getCredentialConfigurations() != null && !isCacheExpired()) {
            Optional<DynamicCredentialConfiguration> first = cachedCCs.getCredentialConfigurations().stream().filter(c -> credentialType.equals(c.credentialType())).findFirst();
            if (first.isPresent()) {
                log.info("Retrieved credential-configuration from cache by credential type: {}", credentialType);
                return first.get();
            }
        }

        try {
            return byobServiceRestClient
                    .get()
                    .uri("v1/admin/credential-configurations/{credential-type}", credentialType)
                    .header(API_KEY, byobServiceProperties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(DynamicCredentialConfiguration.class);
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(BYOB.name(), "IO error when calling Byob-service for credentialType=%s".formatted(credentialType), e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException(BYOB.name(), "server_error", "Failed to get information from Byob-service for credentialType=%s".formatted(credentialType), HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public DynamicCredentialConfiguration searchByCredentialConfigurationId(String credentialConfigurationId) {
        if (credentialConfigurationId == null) {
            log.warn("credentialConfigurationId is null");
            return null;
        }
        if (cachedCCs != null && cachedCCs.getCredentialConfigurations() != null && !isCacheExpired()) {
            Optional<DynamicCredentialConfiguration> first = cachedCCs.getCredentialConfigurations().stream().filter(c -> credentialConfigurationId.equals(c.credentialConfigurationId())).findFirst();
            if (first.isPresent()) {
                log.info("Retrieved credential-configuration from cache by credentialConfigurationId: {}", credentialConfigurationId);
                return first.get();
            }
        }
        try {
            return byobServiceRestClient
                    .get()
                    .uri("v1/admin/credential-configurations/search?credentialConfigurationId={credentialConfigurationId}", credentialConfigurationId)
                    .header(API_KEY, byobServiceProperties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(DynamicCredentialConfiguration.class);
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(BYOB.name(), "IO error when calling Byob-service for credentialConfigurationId=%s".formatted(credentialConfigurationId), e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException(BYOB.name(), "server_error", "Failed to get information from Byob-service for credentialConfigurationId=%s".formatted(credentialConfigurationId), HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data from authoritative source. Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new ClaimsSourceException(BYOB.name(), "server_error", "Failed to get information from Byob-service", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

}
