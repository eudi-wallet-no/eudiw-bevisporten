package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VerificationStartResponse(
        @JsonProperty("authorization_request") String authorizationRequest,
        @JsonProperty("authorization_request_qr_code") String authorizationRequestQrCode,
        @JsonProperty("verifier_transaction_id") String verifierTransactionId) {
}
