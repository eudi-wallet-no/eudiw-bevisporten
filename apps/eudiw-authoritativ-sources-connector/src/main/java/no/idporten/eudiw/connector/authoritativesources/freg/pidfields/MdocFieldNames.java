package no.idporten.eudiw.connector.authoritativesources.freg.pidfields;

public class MdocFieldNames extends PidFieldNames {
    private static final String BIRTH_PLACE = "place_of_birth";

    private static final MdocFieldNames INSTANCE = new MdocFieldNames();

    public static MdocFieldNames getInstance() {
        return INSTANCE;
    }

    @Override
    public String getPlaceOfBirth() {
        return BIRTH_PLACE;
    }
}
