package no.idporten.eudiw.verifier.openid4vp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.Map;

@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerifiedCredential(
        @JsonProperty("claims")
        Map<String, Object> claims
) implements Serializable {
}
