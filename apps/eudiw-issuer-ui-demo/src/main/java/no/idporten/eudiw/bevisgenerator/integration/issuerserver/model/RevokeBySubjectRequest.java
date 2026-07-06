package no.idporten.eudiw.bevisgenerator.integration.issuerserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RevokeBySubjectRequest(
        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,
        @JsonProperty("subject_identifier")
        String subjectIdentifier
) {
}
