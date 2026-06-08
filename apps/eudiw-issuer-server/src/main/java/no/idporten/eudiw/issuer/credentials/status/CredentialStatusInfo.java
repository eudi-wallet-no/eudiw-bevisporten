package no.idporten.eudiw.issuer.credentials.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;

import java.util.List;

public record CredentialStatusInfo(@JsonProperty("credential_issuer_tenant") String credentialIssuerTenant,
                                   @JsonProperty("credential_configuration_id") String credentialConfigurationId,
                                   @JsonProperty("status_entries") List<StatusEntry> statusEntries) {

}
