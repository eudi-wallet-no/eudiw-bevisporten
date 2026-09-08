package no.idporten.eudiw.login;

import no.idporten.eudiw.login.openid4vp.verifier.model.VerifiedCredential;
import tools.jackson.databind.json.JsonMapper;

public class TestData {

    public static String syntheticPersonIdentifier() {
        return "08868797275";
    }

    public static String invalidPersonIdentifier() {
        return "08868797277";
    }


    public static VerifiedCredential verifiedCredentialNO(String personIdentifier) {
        String json = """
                {
                  "valid": true,
                  "claims": {
                    "eu.europa.ec.eudi.pid.1": {
                      "given_name": "LEGITIM",
                      "family_name": "LOMMEBOK",
                      "personal_administrative_number": "%s"
                    }
                  }
                }
                """.formatted(personIdentifier);
        return new JsonMapper().readValue(json, VerifiedCredential.class);
    }

    public static VerifiedCredential verifiedCredentialEU() {
        return verifiedCredentialEUWithPersonalAdministrativeNumber("123456789");
    }

    public static VerifiedCredential verifiedCredentialEUWithPersonalAdministrativeNumber(String personalAdministrativeNumber) {
        String json = """
                {
                  "valid": true,
                  "claims": {
                    "eu.europa.ec.eudi.pid.1": {
                      "place_of_birth": {
                        "country": "FI"
                      },
                      "nationality": [
                        "FI"
                      ],
                      "birth_date": "1996-09-25",
                      "given_name": "UFUNKSJONELL",
                      "family_name": "LOMMEBOK",
                      "issuing_authority": "Finnish Border Guard",
                      "issuing_country": "FI",
                      "personal_administrative_number": "%s"
                    }
                  }
                }
                """.formatted(personalAdministrativeNumber);
        return new JsonMapper().readValue(json, VerifiedCredential.class);
    }

    public static VerifiedCredential verifiedCredentialEUWithoutPersonalAdministrativeNumber() {
        String json = """
                {
                  "valid": true,
                  "claims": {
                    "eu.europa.ec.eudi.pid.1": {
                      "place_of_birth": {
                        "country": "FI"
                      },
                      "nationality": [
                        "FI"
                      ],
                      "birth_date": "1996-09-25",
                      "given_name": "UFUNKSJONELL",
                      "family_name": "LOMMEBOK",
                      "issuing_authority": "Finnish Border Guard",
                      "issuing_country": "FI"
                    }
                  }
                }
                """;
        return new JsonMapper().readValue(json, VerifiedCredential.class);
    }

}
