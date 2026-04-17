package no.idporten.eudiw.issuer.credentials.status.integration;

import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;
import no.idporten.eudiw.issuer.config.APIConnectionProperties;
import no.idporten.eudiw.issuer.credentials.status.StatusIssuerProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@Service
public class StatusIssuerIntegration implements InitializingBean {

    private final StatusIssuerProperties statusIssuerProperties;
    private RestClient restClient;

    public StatusIssuerIntegration(StatusIssuerProperties statusIssuerProperties) {
        this.statusIssuerProperties = statusIssuerProperties;
    }

    public List<StatusEntry> allocateStatusEntries(int numberOfEntries) {
        StatusEntriesRequest statusEntriesRequest = new StatusEntriesRequest(numberOfEntries);
        try {
            StatusEntriesResponse statusEntriesResponse = restClient.post()
                    .uri("/status-issuer/api/v1/entries")
                    .body(statusEntriesRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (_, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (_, response) -> handleErrorResponse(response))
                    .body(StatusEntriesResponse.class);
            if (statusEntriesResponse == null || CollectionUtils.isEmpty(statusEntriesResponse.statusEntries())) {
                throw new CredentialRequestDeniedException("Failed to allocate status for credential", "Status issuer did not return any status entries");
            }
            return statusEntriesResponse.statusEntries();
        } catch (RestClientException e) {
            throw new CredentialRequestDeniedException("Failed to allocate status for credential", "IO exception when calling status issuer", e);
        }
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to allocate status entry from status issuer: status: [%s], message: [%s]".formatted(response.getStatusCode(), body);
        throw new CredentialRequestDeniedException("Failed to allocate status for credential", logMessage);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        APIConnectionProperties apiProperties = this.statusIssuerProperties.getApi();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(apiProperties.connectTimeout());
        requestFactory.setReadTimeout(apiProperties.readTimeout());
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(apiProperties.apiKeyHeader(), apiProperties.apiKey())
                .baseUrl(apiProperties.uri())
                .requestFactory(requestFactory)
                .build();
    }

    protected void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

}
