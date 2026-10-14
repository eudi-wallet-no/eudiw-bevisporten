package no.idporten.eudiw.verifier.openid4vp.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcqlCredentialQuery implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("format")
    private String format;

    @JsonProperty("meta")
    private DcqlCredentialMeta meta;

    @Schema(description = "Expected authorities or trust frameworks, forwarded to the wallet without changing verifier trust validation. Omitted when null or empty.")
    @JsonProperty("trusted_authorities")
    private List<DcqlTrustedAuthoritiesQuery> trustedAuthorities;

    @JsonProperty("require_cryptographic_holder_binding")
    private Boolean requireCryptographicHolderBinding;

    @JsonProperty("claims")
    private List<DcqlClaimQuery> claims;

    @JsonProperty("claim_sets")
    private List<List<String>> claimSets;

    public boolean getRequireCryptographicHolderBinding() {
        if (Objects.isNull(requireCryptographicHolderBinding) || StringUtils.isBlank(requireCryptographicHolderBinding.toString())) {
            return true;
        } else {
            return requireCryptographicHolderBinding;
        }
    }

}
