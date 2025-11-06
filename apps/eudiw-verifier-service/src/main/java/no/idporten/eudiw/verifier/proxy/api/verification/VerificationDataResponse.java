package no.idporten.eudiw.verifier.proxy.api.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerificationDataResponse(
        @JsonProperty("status") String status,
        @JsonProperty("verifier_transaction_id") String verifierTransactionId,
        @JsonProperty("credentials")Map<String, Object> credentials
        ) {
}
