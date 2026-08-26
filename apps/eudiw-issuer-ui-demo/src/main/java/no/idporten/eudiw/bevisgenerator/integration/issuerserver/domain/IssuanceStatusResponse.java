package no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssuanceStatusResponse(
        @JsonProperty("issuance_transaction_id") String issuanceTransactionId,
        @JsonProperty("status") IssuanceStatus status
) {
}
