package no.idporten.eudiw.issuer.credentials.status.cache;

import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;

import java.io.Serializable;
import java.util.List;

public record CredentialStatusInfo(String credentialIssuerTenant,
                                   String credentialConfigurationId,
                                   List<StatusEntry> statusEntries) implements Serializable {
}
