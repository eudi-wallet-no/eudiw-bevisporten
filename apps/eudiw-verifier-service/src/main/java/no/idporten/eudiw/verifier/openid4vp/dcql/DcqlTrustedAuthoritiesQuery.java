package no.idporten.eudiw.verifier.openid4vp.dcql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcqlTrustedAuthoritiesQuery implements Serializable {

    @Schema(description = "Trust framework type identifier, such as aki, etsi_tl or openid_federation. Other type identifiers are forwarded unchanged.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("type")
    private String type;

    @Schema(description = "Non-empty array of identifiers specific to the trusted authorities type.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("values")
    private List<String> values;

}
