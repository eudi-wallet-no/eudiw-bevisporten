package no.idporten.eudiw.issuer.credentials.status.integration;

import no.idporten.eudiw.issuer.claimssource.exception.StatusListException;
import no.idporten.eudiw.issuer.credentials.status.StatusIssuerProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
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
                    .onStatus(HttpStatusCode::is5xxServerError, (_, response) -> handleAllocationErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (_, response) -> handleAllocationErrorResponse(response))
                    .body(StatusEntriesResponse.class);
            if (statusEntriesResponse == null || CollectionUtils.isEmpty(statusEntriesResponse.statusEntries())) {
                throw new StatusListException("Failed to allocate status for credential", "Status issuer did not return any status entries");
            }
            return statusEntriesResponse.statusEntries();
        } catch (RestClientException e) {
            throw new StatusListException("Failed to allocate status for credential", "IO exception when calling status issuer", e);
        }
    }

    public void updateStatusEntries(List<UpdatedStatusEntry> statusEntries) {
        UpdateStatusEntriesRequest updateStatusEntriesRequest = new UpdateStatusEntriesRequest(statusEntries);
        try {
            restClient.put()
                    .uri("/status-issuer/api/v1/entries")
                    .body(updateStatusEntriesRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (_, response) -> handleStatusUpdateErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (_, response) -> handleStatusUpdateErrorResponse(response))
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new StatusListException("Failed to update status for credential", "IO exception when calling status issuer", e);
        }
    }

    void handleAllocationErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to allocate status entry from status issuer: status: [%s], message: [%s]".formatted(response.getStatusCode(), body);
        throw new StatusListException("Failed to allocate status for credential", logMessage);
    }

    void handleStatusUpdateErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to update status entry from status issuer: status: [%s], message: [%s]".formatted(response.getStatusCode(), body);
        throw new StatusListException("Failed to update status for credential", logMessage);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.restClient = statusIssuerProperties.getApi().createRestClient();
    }

    protected void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

}
