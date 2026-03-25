package no.idporten.eudiw.issuer.authoritativesources;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.config.AuthoritativeSourceProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.protocol.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for retrieving credential data from external authoritative sources.
 */
@Service
public class AuthoritativeSourceService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AuthoritativeSourceService.class);

    static final String INVALID_REQUEST = "invalid_request";
    static final String SERVER_ERROR = "server_error";
    static final String INVALID_CLAIMS_DATA = "invalid_claims_data";
    static final String CREDENTIAL_ISSUANCE_DENIED = "credential_data_denied";
    static final String FAILED_CREDENTIAL_REQUEST = "credential_data_retrieval_failed";
    static final String NOT_FOUND_CREDENTIAL_DATA = "credential_data_not_found";

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final JsonMapper jsonMapper;
    private final Map<String, RestClient> restClients = new HashMap<>();

    public AuthoritativeSourceService(CredentialIssuerServerProperties credentialIssuerServerProperties, JsonMapper jsonMapper) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.jsonMapper = jsonMapper;
    }

    private AuthoritativeSourceProperties findAuthoritativeSourceProperties(String source) {
        return Optional.ofNullable(credentialIssuerServerProperties.getAuthoritativeSources().get(source))
                .orElseThrow(() -> new IssuerServerException(
                        "server_error",
                        "Unknown authoritative source",
                        "Unknown authoritative source [%s]".formatted(source),
                        HttpStatus.INTERNAL_SERVER_ERROR)
                );
    }

    public CredentialData retrieveCredentialData(final String source, final String credentialType, final String personIdentifier) {
        Subject subject = new Subject(personIdentifier);
        AuthoritativeSourceRequest authoritativeSourceRequest = new AuthoritativeSourceRequest(subject, credentialType);
        AuthoritativeSourceProperties authoritativeSourceProperties = findAuthoritativeSourceProperties(source);
        AuthoritativeSourceResponse authoritativeSourceResponse = restClients.get(source)
                .post()
                .uri(authoritativeSourceProperties.uri())
                .body(authoritativeSourceRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (_, response) -> handleErrorResponse(source, response))
                .onStatus(HttpStatusCode::is4xxClientError, (_, response) -> handleErrorResponse(source, response))
                .body(AuthoritativeSourceResponse.class);
        if  (authoritativeSourceResponse == null || CollectionUtils.isEmpty(authoritativeSourceResponse.credentialData())) {
            throw new ClaimsSourceDataNotFoundException(source, "No data available", "Authoritative source returned no data");
        }
        return new CredentialData(authoritativeSourceResponse.credentialData());
    }

    void handleErrorResponse(String source, ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data fra authoritative source [{%s}]: status: [%s], message: [%s]".formatted(source, response.getStatusCode(), body);
        AuthoritativeSourceErrorResponse errorResponse;
        try {
            errorResponse = jsonMapper.readValue(body, AuthoritativeSourceErrorResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse error response body from authoritative source [{}]", source, e);
            throw new ClaimsSourceException(source, SERVER_ERROR, "Failed to retrieve credential data from authoritative source", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
        }
        switch(errorResponse.error()) {
            case SERVER_ERROR:
                throw new ClaimsSourceException(source, "server_error", errorResponse.errorDescription(), HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
            case INVALID_REQUEST:
            case CREDENTIAL_ISSUANCE_DENIED:
            case FAILED_CREDENTIAL_REQUEST:
            case NOT_FOUND_CREDENTIAL_DATA:
            case INVALID_CLAIMS_DATA:
                throw new ClaimsSourceInvalidDataException(source, errorResponse.errorDescription(), logMessage);
        }
        throw new ClaimsSourceInvalidDataException(source, errorResponse.errorDescription(), logMessage);
    }

    private RestClient createRestClient(AuthoritativeSourceProperties properties) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(properties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Map.Entry<String, AuthoritativeSourceProperties> entry : credentialIssuerServerProperties.getAuthoritativeSources().entrySet()) {
            restClients.put(entry.getKey(), createRestClient(entry.getValue()));
            log.info("Initialized authoritative source {} with URI {}", entry.getKey(), entry.getValue().uri());
        }
    }

}
