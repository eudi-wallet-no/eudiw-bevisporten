package no.idporten.eudiw.issuer.openid4vci.protocol;

import com.nimbusds.openid.connect.sdk.Nonce;
import lombok.Getter;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidProof extends IssuerServerException {

    public static final String INVALID_PROOF = "invalid_proof";

    private final Nonce nonce;
    private final long nonceExpiresInSecpnds = 60 * 60 * 24;

    public InvalidProof(Nonce nonce, String errorDescription, Throwable cause) {
        super(INVALID_PROOF, errorDescription, HttpStatus.BAD_REQUEST, cause);
        this.nonce = nonce == null ? new Nonce() : nonce;
    }

    public InvalidProof(Nonce nonce, String errorDescription) {
        this(nonce, errorDescription, null);
    }

    public InvalidProof(String errorDescription) {
        this(null, errorDescription);
    }

    public InvalidProof(String errorDescription, Throwable cause) {
        this(null, errorDescription, cause);
    }

}
