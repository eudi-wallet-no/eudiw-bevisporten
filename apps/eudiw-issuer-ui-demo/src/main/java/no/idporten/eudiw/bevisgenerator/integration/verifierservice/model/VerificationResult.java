package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VerificationResult(
        @JsonProperty("verifier_transaction_id") String verifierTransactionId,
        @JsonProperty("credentials") Map<String, List<CredentialPresentation>> credentials
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CredentialPresentation(
            @JsonProperty("claims") Map<String, Object> claims
    ) {
    }
}
