package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.jwk.JWK;
import lombok.Data;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;

@Data
public class VerificationTransaction implements Serializable {

    private ClientApplication clientApplication;
    private DcqlQuery dcqlQuery;
    private URI redirectUri;
    private String flow;
    private String status;
    private String state;
    private String nonce;
    private JWK encryptionKey;
    private VerifiedCredentials verifiedCredentials;
    private Map<String, Object> request;
    private Map<String, Object> response;

}
