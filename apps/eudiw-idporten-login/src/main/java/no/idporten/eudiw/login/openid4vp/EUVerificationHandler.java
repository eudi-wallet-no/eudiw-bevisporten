package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerifiedCredential;
import no.idporten.sdk.oidcserver.protocol.Authorization;

import java.util.UUID;

/**
 * Handler for EU PID verification.
 */
public class EUVerificationHandler implements OpenID4VPVerificationHandler {

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
                      "path" : [ "eu.europa.ec.eudi.pid.1", "birth_date" ]
                    }, {
                      "path" : [ "eu.europa.ec.eudi.pid.1", "place_of_birth" ]
                    }, {
                      "path" : [ "eu.europa.ec.eudi.pid.1", "nationality" ]
                    } ],
                    "id" : "%s"
                  } ]
                }""".formatted(walletInteractionId);
        return DcqlQuery.parse(dcql);
    }

    @Override
    public Authorization completeVerification(VerifiedCredential verifiedCredential) {
        return Authorization.builder()
                .sub(UUID.randomUUID().toString()) // TODO EUW-1672 - Finne egnet verdi for sub for innlogging med europeisk PID
                .acr(AcrValue.EIDAS_LOA_HIGH.value())
                .amr(AMR_EUDIW)
                .attribute("family_name", verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "family_name"))
                .attribute("given_name", verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "given_name"))
                .attribute("birthdate", verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "birth_date"))
                .build();
    }

}