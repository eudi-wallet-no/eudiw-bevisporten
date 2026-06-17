package no.idporten.eudiw.verifier.proxy.openid4vp;

import com.nimbusds.jose.jwk.JWK;
import lombok.Data;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.VerifiedCredentials;

import java.util.Map;

@Data
public class VerificationTransaction {

    private Map<String, Object> dcqlQuery;

    private String status;
    private String state;
    private String nonce;
    private JWK encryptionKey;
    private VerifiedCredentials verifiedCredentials;

}
