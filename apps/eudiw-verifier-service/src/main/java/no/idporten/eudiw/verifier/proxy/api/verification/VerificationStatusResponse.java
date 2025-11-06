package no.idporten.eudiw.verifier.proxy.api.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerificationStatusResponse(
        @JsonProperty("status") String status,
        @JsonProperty("verifier_transaction_id") String verifierTransactionId
) {
}
