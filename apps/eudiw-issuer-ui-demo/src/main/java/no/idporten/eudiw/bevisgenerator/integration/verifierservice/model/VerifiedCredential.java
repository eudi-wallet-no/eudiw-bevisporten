package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        Boolean valid,
        @JsonProperty("validation_details")
        List<ValidationDetail> validationDetails
) implements Serializable {

    public boolean isValid() {
        return Boolean.TRUE.equals(valid);
    }
}
