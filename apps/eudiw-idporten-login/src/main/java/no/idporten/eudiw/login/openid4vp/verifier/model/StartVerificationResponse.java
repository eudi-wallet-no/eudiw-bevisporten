package no.idporten.eudiw.login.openid4vp.verifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record StartVerificationResponse(
        @JsonProperty("authorization_request")
        URI authorizationRequest,
        @JsonProperty("authorization_request_qr_code")
        URI authorizationRequestQrCode,
        @JsonProperty("verifier_transaction_id")
        String verifierTransactionId
) {
}
