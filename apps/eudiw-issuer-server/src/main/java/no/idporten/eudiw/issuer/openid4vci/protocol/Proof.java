package no.idporten.eudiw.issuer.openid4vci.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import lombok.Data;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;

import java.text.ParseException;

@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Proof {
    @JsonProperty("jwt")
    private String jwt;
    @JsonProperty("proof_type")
    private String proofType;

    // TODO ordentlig implementering av proofs er nødvendig, denne er litt for basic og det finnes flere typer
    // TODO hva skal validere signatur med
    // TODO
    public void validate() {
        JWK jwk = getBindingKey();
        if (jwk == null) {
            throw new IssuerServerException("invalid_proof", "Missing binding key", HttpStatus.BAD_REQUEST);
        }
    }

    public JWK getBindingKey() {
        try {
            JWT jwt = SignedJWT.parse(getJwt());
            JWSHeader jwsHeader = (JWSHeader) jwt.getHeader();
            return jwsHeader.getJWK();
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_proof", "Invalid proof format", HttpStatus.BAD_REQUEST);
        }
    }

}
