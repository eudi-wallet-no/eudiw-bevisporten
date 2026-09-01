package no.idporten.eudiw.verifier.openid4vp;


import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;

import java.security.cert.X509Certificate;

public interface Format {
    VerifiedCredential handle(String vpToken, boolean includeValidationDetails);
    ValidationStatus checkTrustlist(X509Certificate cert);
    ValidationStatus checkStatuslist(StatuslistEntry statuslistEntry);
}
