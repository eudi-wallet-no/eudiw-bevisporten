package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerifiedCredential;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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
                      "id": "family_name", "path" : [ "eu.europa.ec.eudi.pid.1", "family_name" ]
                    }, {
                      "id": "given_name", "path" : [ "eu.europa.ec.eudi.pid.1", "given_name" ]
                    }, {
                      "id": "birth_date", "path" : [ "eu.europa.ec.eudi.pid.1", "birth_date" ]
                    }, {
                      "id": "place_of_birth", "path" : [ "eu.europa.ec.eudi.pid.1", "place_of_birth" ]
                    }, {
                      "id": "nationality", "path" : [ "eu.europa.ec.eudi.pid.1", "nationality" ]
                    }, {
                      "id": "issuing_authority", "path" : [ "eu.europa.ec.eudi.pid.1", "issuing_authority" ]
                    }, {
                      "id": "issuing_country", "path" : [ "eu.europa.ec.eudi.pid.1", "issuing_country" ]
                    }, {
                      "id": "personal_administrative_number", "path" : [ "eu.europa.ec.eudi.pid.1", "personal_administrative_number" ]
                    } ],
                    "claim_sets": [
                            ["family_name", "given_name", "birth_date", "place_of_birth", "nationality", "issuing_authority", "issuing_country", "personal_administrative_number"],
                            ["family_name", "given_name", "birth_date", "place_of_birth", "nationality", "issuing_authority", "issuing_country"] ],
                    "id" : "%s"
                  } ]
                }""".formatted(walletInteractionId);
        return DcqlQuery.parse(dcql);
    }

    @Override
    public Authorization completeVerification(VerifiedCredential verifiedCredential) {
        String familyName = verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "family_name");
        String givenName = verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "given_name");
        String birthDate = verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "birth_date");
        Map<String, Serializable> birthPlace = verifiedCredential.getObjectClaim(true, "eu.europa.ec.eudi.pid.1", "place_of_birth");
        String birthPlaceCountry = (String) birthPlace.get("country");
        String birthPlaceRegion = (String) birthPlace.get("region");
        String birthPlaceLocality = (String) birthPlace.get("locality");
        List<String> nationality = verifiedCredential.getStringArrayClaim(true, "eu.europa.ec.eudi.pid.1", "nationality");
        String issuingAuthority = verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "issuing_authority");
        String issuingCountry = verifiedCredential.getStringClaim(true, "eu.europa.ec.eudi.pid.1", "issuing_country");
        String personalAdministrativeNumber = verifiedCredential.getStringClaim(false, "eu.europa.ec.eudi.pid.1", "personal_administrative_number");
        String sub = StringUtils.hasText(personalAdministrativeNumber) ?
                calculateSubHash(personalAdministrativeNumber, issuingAuthority, issuingCountry)
                :
                calculateSubHash(familyName, givenName, birthDate, birthPlaceCountry, birthPlaceRegion, birthPlaceLocality, nationality.toString(),  issuingAuthority, issuingCountry);

        return Authorization.builder()
                .sub(sub)
                .acr(AcrValue.EIDAS_LOA_HIGH.value())
                .amr(AMR_EUDIW)
                .attribute("family_name", familyName)
                .attribute("given_name", givenName)
                .attribute("birthdate", birthDate)
                .attribute("place_of_birth", (Serializable) birthPlace)
                .attribute("nationalities", (Serializable) nationality)
                .attribute("issuing_authority", issuingAuthority)
                .attribute("issuing_country", issuingCountry)
                .attribute("personal_administrative_number", personalAdministrativeNumber)
                .build();
    }

    /**
     * Calculates a sub by hashing attributes.  The result is base64-encoded.
     * @param attributes attributes from pid
     * @return a calculated sub value
     */
    protected String calculateSubHash(String ... attributes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String attribute : attributes) {
                if (attribute != null) {
                    digest.update(attribute.getBytes());
                }
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}