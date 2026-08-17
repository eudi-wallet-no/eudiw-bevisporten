package no.idporten.eudiw.connector.authoritativesources.freg.pidfields;

public class SdJwtFieldNames extends PidFieldNames {
    private static final String BIRTH_DATE = "birthdate";
    private static final String BIRTH_PLACE = "place_of_birth";
    private static final String NATIONALITY = "nationalities";

    private static final SdJwtFieldNames INSTANCE = new SdJwtFieldNames();

    public static SdJwtFieldNames getInstance() {
        return INSTANCE;
    }

    @Override
    public String getBirthDate() {
        return BIRTH_DATE;
    }

    @Override
    public String getPlaceOfBirth() {
        return BIRTH_PLACE;
    }

    @Override
    public String getNationality() {
        return NATIONALITY;
    }
}
