package no.idporten.eudiw.connector.authoritativesources.freg.pidfields;

public class PidFieldNames {
    private final static String PERSONAL_ADMINISTRATIVE_NUMBER = "personal_administrative_number";
    private final static String FAMILY_NAME = "family_name";
    private final static String GIVEN_NAME = "given_name";
    private final static String BIRTH_DATE = "birth_date";
    private final static String BIRTH_PLACE = "place_of_birth";
    private final static String NATIONALITY = "nationality";
    private final static String EXPIRY_DATE = "expiry_date";
    private final static String ISSUING_AUTHORITY = "issuing_authority";
    private final static String ISSUING_COUNTRY = "issuing_country";


    public String getPersonalAdministrativeNumber() {
        return PERSONAL_ADMINISTRATIVE_NUMBER;
    }

    public String getFamilyName() {
        return FAMILY_NAME;
    }

    public String getGivenName() {
        return GIVEN_NAME;
    }

    public String getBirthDate() {
        return BIRTH_DATE;
    }

    public String getPlaceOfBirth() {
        return BIRTH_PLACE;
    }

    public String getNationality() {
        return NATIONALITY;
    }

    public String getExpiryDate() {
        return EXPIRY_DATE;
    }

    public String getIssuingAuthority() {
        return ISSUING_AUTHORITY;
    }

    public String getIssuingCountry() {
        return ISSUING_COUNTRY;
    }
}
