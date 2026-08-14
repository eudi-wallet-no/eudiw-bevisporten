package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VerificationStatus(
        @JsonProperty("status")
        String status,

        @JsonProperty("verifier_transaction_id")
        String verifierTransactionId
) { }
