package no.idporten.eudiw.issuer.openid4vci.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claims description for issuer metadata - https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#claims-description-issuer-metadata
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProofTypes {

    @JsonProperty("jwt")
    private ProofType jwtProofType;

    @JsonProperty("attestation")
    private ProofType attestationProofType;

}
