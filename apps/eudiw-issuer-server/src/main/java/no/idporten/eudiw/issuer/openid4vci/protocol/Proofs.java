package no.idporten.eudiw.issuer.openid4vci.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import lombok.Data;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;
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
            throw new IssuerServerException("invalid_proof", "Missing binding keys", HttpStatus.BAD_REQUEST);
        }
        getBindingKeys();
    }

    public List<JWK> getBindingKeys() {
        List<JWK> bindingKeys = new ArrayList<>();
        for (String jwt : getJwt()) {
            try {
                bindingKeys.add(SignedJWT.parse(jwt).getHeader().getJWK());
            } catch (ParseException e) {
                throw new IssuerServerException("invalid_proof", "Invalid proof format", HttpStatus.BAD_REQUEST);
            }
        }
        return bindingKeys;
    }

}
