package no.idporten.eudiw.verifier.openid4vp.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcqlCredentialMeta {

    @JsonProperty("vct_values")
    private List<String> vctValues;

    @JsonProperty("doctype_value")
    private String doctypeValue;

    @JsonProperty("doctype_values")
    private List<String> doctypeValues;

}
