package no.idporten.eudiw.login.openid4vp.verifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DcqlCredentialMeta(
        @JsonProperty("vct_values")
        List<String> vctValues,
        @JsonProperty("doctype_value")
        String doctypeValue,
        @JsonProperty("doctype_values")
        List<String> doctypeValues
) {
}
