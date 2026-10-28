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
        boolean multiple,
        DcqlCredentialMeta meta,
        @JsonProperty("trusted_authorities")
        List<TrustedAuthorityQuery> trustedAuthorities,
        boolean require_cryptographic_holder_binding,
        List<DcqlClaimQuery> claims,
        @JsonProperty("claim_sets")
        List<List<String>> claimSets
) {
    public DcqlCredentialQuery withTrustedAuthorities(List<TrustedAuthorityQuery> trustedAuthorities) {
        return new DcqlCredentialQuery(
                id,
                format,
                multiple,
                meta,
                trustedAuthorities,
                require_cryptographic_holder_binding,
                claims,
                claimSets);
    }
}
