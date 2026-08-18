package no.idporten.eudiw.verifier.openid4vp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationDetail;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerifiedCredential (
        @JsonProperty("claims")
        Map<String, Object> claims,
        @JsonProperty("valid")
        boolean valid,
        @JsonProperty("validation_details")
        List<ValidationDetail> validationDetails
) implements Serializable {
}
