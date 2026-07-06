package no.idporten.eudiw.login.openid4vp.verifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DcqlCredentialQuery(
        String id,
        String format,
        DcqlCredentialMeta meta,
        List<DcqlClaimQuery> claims,
        @JsonProperty("claim_sets")
        List<List<String>> claimSets
) {
}
