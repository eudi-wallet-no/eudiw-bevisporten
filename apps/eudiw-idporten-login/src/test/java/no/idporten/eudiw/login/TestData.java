package no.idporten.eudiw.login;

import no.idporten.eudiw.login.openid4vp.verifier.model.VerifiedCredential;

import java.util.List;
import java.util.Map;

public class TestData {

    public static String syntheticPersonIdentifier() {
        return "08868797275";
    }

    public static VerifiedCredential verifiedCredentialNO() {
        Map<String, Object> claims = Map.of(
                "personal_administrative_number", syntheticPersonIdentifier(),
                "family_name", "LOMMEBOK",
                "given_name", "LEGITIM"
        );
        return new VerifiedCredential(claims);
    }

    public static VerifiedCredential verifiedCredentialEU() {
        Map<String, Object> claims = Map.of(
                "family_name", "LOMMEBOK",
                "given_name", "UFUNKSJONELL",
                "birth_date", "996-09-25",
                "place_of_birth", Map.of("country", "FI"),
                "nationality", List.of("FI")
        );
        return new VerifiedCredential(claims);
    }

}
