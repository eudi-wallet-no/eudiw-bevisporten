package no.idporten.eudiw.verifier.proxy.api.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record StartVerificationResponse(
        @JsonProperty("authorization_request") URI authorizationRequest,
        @JsonProperty("verifier_transaction_id") String verifierTransactionId
) {
}
