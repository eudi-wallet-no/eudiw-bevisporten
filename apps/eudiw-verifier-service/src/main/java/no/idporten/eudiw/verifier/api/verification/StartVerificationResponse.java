package no.idporten.eudiw.verifier.api.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;

@Schema(description = "Start verification response", title = "Start credential verification response", type = "object")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record StartVerificationResponse(
        @Schema(description = "Authorization request. Embed in qr code or link.", example = "eudi-openid4vp://...")
        @JsonProperty("authorization_request") URI authorizationRequest,

        @Schema(description = "Verification transaction id. Use for status and result retrieval.", example = "xyz...")
        @JsonProperty("verifier_transaction_id") String verifierTransactionId
) {
}
