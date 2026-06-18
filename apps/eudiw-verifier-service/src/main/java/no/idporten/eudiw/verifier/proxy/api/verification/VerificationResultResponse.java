package no.idporten.eudiw.verifier.proxy.api.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.idporten.eudiw.verifier.proxy.openid4vp.VerifiedCredential;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerificationResultResponse(
        @Schema(description = "Verification transaction id.", example = "xyz...")
        @JsonProperty("verifier_transaction_id") String verifierTransactionId,
        @Schema(description = "Verified credentials.")
        @JsonProperty("credentials") Map<String, List<VerifiedCredential>> credentials
        ) {
}
