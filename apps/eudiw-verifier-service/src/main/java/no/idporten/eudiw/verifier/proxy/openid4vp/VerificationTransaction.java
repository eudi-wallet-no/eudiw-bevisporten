package no.idporten.eudiw.verifier.proxy.openid4vp;

import com.nimbusds.jose.jwk.JWK;
import lombok.Data;
import no.idporten.eudiw.verifier.proxy.openid4vp.dcql.DcqlQuery;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.VerifiedCredentials;

@Data
public class VerificationTransaction {

    private DcqlQuery dcqlQuery;

    private String status;
    private String state;
    private String nonce;
    private JWK encryptionKey;
    private VerifiedCredentials verifiedCredentials;

}
