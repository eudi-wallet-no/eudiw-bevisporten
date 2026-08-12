package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerifiedCredential;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.validators.identifier.PersonIdentifierValidator;

/**
 * Handler for Norwegian PID verification.
 */
public class NorwegianVerificationHandler implements OpenID4VPVerificationHandler {

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
                .sub(getPersonIdentifier(verifiedCredential))
                .acr(AcrValue.IDPORTEN_LOA_HIGH.value())
                .amr(AMR_EUDIW)
                .attribute("family_name", verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "family_name"))
                .attribute("given_name", verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "given_name"))
                .build();
    }

    private String getPersonIdentifier(VerifiedCredential verifiedCredential) {
        String personIdentifier = verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "personal_administrative_number");
        if (! PersonIdentifierValidator.isValid(personIdentifier)) {
            throw new InvalidVerificationException("Invalid person identifier");
        }
        return personIdentifier;
    }

}