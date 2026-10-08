package no.idporten.eudiw.verifier.openid4vp.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcqlClaimQuery implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("path")
    private List<String> path;

    @Schema(description = "Whether the requested mdoc claim will be retained. Defaults to true for mdoc claims when omitted or null. Ignored and omitted for other credential formats.")
    @JsonProperty("intent_to_retain")
    private Boolean intentToRetain;

}
