package no.idporten.eudiw.bevisgenerator.integration.issuerserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssuanceDefinition(
        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,
        @JsonProperty("subject")
        IssuanceSubject subject,
        @JsonProperty("credential_data")
        IssuanceCredentialData credentialData
) {
    public IssuanceDefinition(String credentialConfigurationId, String id, IssuanceCredentialData credentialData) {
        this(credentialConfigurationId, new IssuanceSubject(id),  credentialData);
    }
}
