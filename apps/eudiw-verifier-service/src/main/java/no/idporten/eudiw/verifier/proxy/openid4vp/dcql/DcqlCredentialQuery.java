package no.idporten.eudiw.verifier.proxy.openid4vp.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcqlCredentialQuery {

    @JsonProperty("id")
    private String id;

    @JsonProperty("format")
    private String format;

    @JsonProperty("meta")
    private DcqlCredentialMeta meta;

    @JsonProperty("claims")
    private List<DcqlClaimQuery> claims;

    @JsonProperty("claim_sets")
    private List<List<String>> claimSets;

}
