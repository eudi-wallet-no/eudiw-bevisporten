package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerifiedCredential;
import no.idporten.sdk.oidcserver.protocol.Authorization;

/**
 * Handler for Norwegian PID verification.
 */
public class NOVerificationHandler implements VerificationHandler {

    @Override
    public DcqlQuery createDcqlQuery(String walletInteractionId) {
        String dcql = """
                {
                  "credentials" : [ {
                    "meta" : {
                      "doctype_value" : "eu.europa.ec.eudi.pid.1"
                    },
                    "format" : "mso_mdoc",
                    "multiple": false,
                    "require_cryptographic_holder_binding": true,
                    "claims" : [ {
                      "path" : [ "eu.europa.ec.eudi.pid.1", "family_name" ]
                    }, {
                      "path" : [ "eu.europa.ec.eudi.pid.1", "given_name" ]
                    }, {
                      "path" : [ "eu.europa.ec.eudi.pid.1", "personal_administrative_number" ]
                    } ],
                    "id" : "%s"
                  } ]
                }""".formatted(walletInteractionId);
        return DcqlQuery.parse(dcql);
    }

    @Override
    public Authorization completeVerification(VerifiedCredential verifiedCredential) {
        return Authorization.builder()
                .sub((String) verifiedCredential.getStringClaim("personal_administrative_number", true))
                .acr(AcrValue.IDPORTEN_LOA_HIGH.value())
                .amr(AMR_EUDIW)
                .attribute("family_name", verifiedCredential.getStringClaim("family_name", true))
                .attribute("given_name", verifiedCredential.getStringClaim("given_name",  true))
                .build();
    }

}