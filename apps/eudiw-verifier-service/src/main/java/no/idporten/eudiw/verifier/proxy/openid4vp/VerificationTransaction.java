package no.idporten.eudiw.verifier.proxy.openid4vp;

import lombok.Data;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.CredentialConfiguration;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.VerifiedCredentials;

@Data
public class VerificationTransaction {

    private String status;
    private CredentialConfiguration credentialConfiguration;
    private VerifiedCredentials verifiedCredentials;

}
