package no.idporten.eudiw.issuer.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nimbusds.openid.connect.sdk.Nonce;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.constraints.NotNull;

public record NonceErrorResponse(
        @NotNull
        @JsonProperty("error")
        String error,
        @NotNull
        @JsonProperty("error_description")
        String errorDescription,
        @NotNull
        @JsonProperty("c_nonce")
        @JsonSerialize(using = ToStringSerializer.class)
        Nonce nonce,
        @NotNull
        @JsonProperty("c_nonce_expires_in")
        long nonceExpiresIn
) {

    public NonceErrorResponse(
            String error,
            String errorDescription,
            Nonce nonce,
            long nonceExpiresIn

    ) {
        this.error = error;
        this.errorDescription = String.format("%s (%s)", errorDescription, Span.current().getSpanContext().getTraceId());
        this.nonce = nonce;
        this.nonceExpiresIn = nonceExpiresIn;
    }

}
