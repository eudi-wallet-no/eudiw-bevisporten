package no.idporten.eudiw.bevisgenerator.integration.issuerserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IssuanceDefinition(
        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,
        IssuanceSubject subject,
        List<IssuanceClaim> claims
) {
    public IssuanceDefinition(String credentialConfigurationId, String id, List<IssuanceClaim> claims) {
        this(credentialConfigurationId, new IssuanceSubject(id),  claims);
    }
}
