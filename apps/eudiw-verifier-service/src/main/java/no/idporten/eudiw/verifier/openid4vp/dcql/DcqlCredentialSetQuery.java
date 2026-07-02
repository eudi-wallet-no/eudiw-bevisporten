package no.idporten.eudiw.verifier.openid4vp.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcqlCredentialSetQuery implements Serializable {

    @JsonProperty("options")
    private List<List<String>> options;

    @JsonProperty("required")
    private Boolean required;

}
