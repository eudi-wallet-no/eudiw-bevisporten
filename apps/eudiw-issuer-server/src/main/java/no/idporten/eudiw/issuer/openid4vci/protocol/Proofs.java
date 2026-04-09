package no.idporten.eudiw.issuer.openid4vci.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import lombok.Data;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.exception.ErrorCode;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Proofs {

    @JsonProperty("jwt")
    private List<String> jwt;

    public void validate() {
        if (CollectionUtils.isEmpty(jwt)) {
            throw new IssuerServerException(ErrorCode.INVALID_PROOF, "Missing binding keys");
        }
        getBindingKeys();
    }

    public List<JWK> getBindingKeys() {
        List<JWK> bindingKeys = new ArrayList<>();
        for (String jwt : getJwt()) {
            try {
                bindingKeys.add(SignedJWT.parse(jwt).getHeader().getJWK());
            } catch (ParseException e) {
                throw new IssuerServerException(ErrorCode.INVALID_PROOF, "Invalid proof format");
            }
        }
        return bindingKeys;
    }

}
