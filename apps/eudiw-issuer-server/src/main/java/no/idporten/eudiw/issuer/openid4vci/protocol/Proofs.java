package no.idporten.eudiw.issuer.openid4vci.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.util.CollectionUtils;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Proofs {

    // https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-jwt-proof-type
    @JsonProperty("jwt")
    private List<String> jwt;

    // https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-attestation-proof-type
    @JsonProperty("attestation")
    private List<String> attestation;

    public void validate() {
        if (CollectionUtils.isEmpty(jwt) && CollectionUtils.isEmpty(attestation)) {
            throw new IssuerServerException(ErrorCode.INVALID_PROOF, "Missing proofs");
        }
    }

}
