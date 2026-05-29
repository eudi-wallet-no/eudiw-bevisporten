package no.idporten.eudiw.bevisgenerator.integration.issuerserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RevokeRequest(
        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,
        @JsonProperty("issuance_transaction_id")
        String issuanceTransactionId
) {
}
