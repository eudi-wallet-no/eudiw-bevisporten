package no.idporten.eudiw.verifier.proxy.openid4vp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerifiedCredential(
        @JsonProperty("claims")
        Map<String, Object> claims
) {
}
