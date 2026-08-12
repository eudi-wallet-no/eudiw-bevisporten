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
        String json = """
                {
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
                      "family_name": "LOMMEBOK"
                    }
                  }
                }
                """;
        return new JsonMapper().readValue(json, VerifiedCredential.class);
    }

}
