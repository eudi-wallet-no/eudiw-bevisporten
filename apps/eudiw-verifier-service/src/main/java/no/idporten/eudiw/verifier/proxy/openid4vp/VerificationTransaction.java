package no.idporten.eudiw.verifier.proxy.openid4vp;

import lombok.Data;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.CredentialConfiguration;

import java.util.Map;

@Data
public class VerificationTransaction {

    private String status;
    private CredentialConfiguration credentialConfiguration;
    private Map<String, Object> verifiedCredentials;

}
