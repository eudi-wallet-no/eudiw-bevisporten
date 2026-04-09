package no.idporten.eudiw.issuer.authoritativesources;

import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final Map<String, RestClient> restClients = new HashMap<>();

    public AuthoritativeSourceService(CredentialIssuerServerProperties credentialIssuerServerProperties) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
    }

    private AuthoritativeSourceProperties findAuthoritativeSourceProperties(String source) {
        return Optional.ofNullable(credentialIssuerServerProperties.getAuthoritativeSources().get(source))
                .orElseThrow(() -> new IssuerServerException(
                        ErrorCode.SERVER_ERROR,
                        "Unknown authoritative source",
                        "Unknown authoritative source [%s]".formatted(source))
                );
    }

    public CredentialData retrieveCredentialData(final String source, final String credentialType, final String personIdentifier) {
        Subject subject = new Subject(personIdentifier);
        AuthoritativeSourceRequest authoritativeSourceRequest = new AuthoritativeSourceRequest(subject, credentialType);
        AuthoritativeSourceProperties authoritativeSourceProperties = findAuthoritativeSourceProperties(source);
        try {
            AuthoritativeSourceResponse authoritativeSourceResponse = restClients.get(source)
                    .post()
                    .uri(authoritativeSourceProperties.uri())
                    .body(authoritativeSourceRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (_, response) -> handleErrorResponse(source, response))
                    .onStatus(HttpStatusCode::is4xxClientError, (_, response) -> handleErrorResponse(source, response))
                    .body(AuthoritativeSourceResponse.class);
            if (authoritativeSourceResponse == null || CollectionUtils.isEmpty(authoritativeSourceResponse.credentialData())) {
                throw new CredentialRequestDeniedException(credentialType, "No data available", "Authoritative source returned no data");
            }
            return new CredentialData(authoritativeSourceResponse.credentialData());
        } catch (ResourceAccessException e) {
            throw new AuthoritativeSourceIOException(source, "IO error when calling source %s".formatted(source), e);
        } catch (RestClientException e) {
            throw new AuthoritativeSourceIOException(source, "Failed to get information from authoritative source %s".formatted(source), e);
        }
    }

    void handleErrorResponse(String source, ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data from authoritative source [%s]: status: [%s], message: [%s]".formatted(source, response.getStatusCode(), body);
        if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new CredentialRequestDeniedException(source, "No credential data available from authoritative source %s".formatted(source), logMessage);
        }
        throw new CredentialRequestDeniedException(source, "Failed to get information from authoritative source %s".formatted(source), logMessage);
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

    protected void addRestClient(String source, RestClient restClient) {
        restClients.put(source, restClient);
    }

}
