package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerifiedCredential;
import no.idporten.sdk.oidcserver.protocol.Authorization;

public interface OpenID4VPVerificationHandler {

   String AMR_EUDIW = "EUDIW";

    static OpenID4VPVerificationHandler forAcrValue(AcrValue acrValue) {
        if (acrValue.isEuLogin()) {
            return new EUVerificationHandler();
        } else {
            return new NorwegianVerificationHandler();
        }
    }

    DcqlQuery createDcqlQuery(String walletInteraction);

    Authorization completeVerification(VerifiedCredential verifiedCredential);

}
