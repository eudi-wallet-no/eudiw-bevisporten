package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.openid.connect.sdk.Nonce;
import lombok.Getter;
import no.idporten.eudiw.issuer.IssuerServerException;

import static no.idporten.eudiw.issuer.ErrorCode.INVALID_PROOF;

@Getter
public class InvalidProof extends IssuerServerException {

    private final Nonce nonce;
    private final long nonceExpiresInSeconds = 60 * 60 * 24;

    public InvalidProof(Nonce nonce, String errorDescription, Throwable cause) {
        super(INVALID_PROOF, errorDescription, cause);
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
