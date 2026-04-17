package no.idporten.eudiw.issuer.credentials.status;

import no.idporten.eudiw.issuer.credentials.status.integration.StatusIssuerIntegration;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatusIssuerService {

    private final StatusIssuerProperties statusIssuerProperties;
    private final StatusIssuerIntegration statusIssuerIntegration;

    public StatusIssuerService(StatusIssuerProperties statusIssuerProperties, StatusIssuerIntegration statusIssuerIntegration) {
        this.statusIssuerProperties = statusIssuerProperties;
        this.statusIssuerIntegration = statusIssuerIntegration;
    }

    public boolean isEnabled() {
        return statusIssuerProperties.isEnabled();
    }

    /**
     * Allocate status list entries.
     */
    public List<CredentialStatus> allocateStatus(int numberOfEntries) {
        return statusIssuerIntegration.allocateStatusEntries(numberOfEntries)
                .stream()
                .map(statusEntry -> CredentialStatus.create(statusEntry.idx(), statusEntry.uri()))
                .toList();
    }

}
