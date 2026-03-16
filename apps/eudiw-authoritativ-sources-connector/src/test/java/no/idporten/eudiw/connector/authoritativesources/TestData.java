package no.idporten.eudiw.connector.authoritativesources;

import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.krr.model.Kontaktinformasjon;
import no.idporten.eudiw.connector.authoritativesources.krr.model.PersonKrr;

public class TestData {
    public static String getValidSyntheticPersonIdentifier() {
        return "50917500484";
    }

    public static String getInvalidSyntheticPersonIdentifier() {
        return "5091750048";
    }

    public static String getTestEmailAddress() {
        return "test@default.digdir.no";
    }

    public static String getTestPhoneNumber() {
        return "12345678";
    }

    public static PersonKrr getValidPersonKrr() {
        return new PersonKrr(
                getValidSyntheticPersonIdentifier(),
                "NEI",
                "AKTIV",
                "KAN_VARSLES",
                getValidKontaktinformasjon()
        );
    }

   public static PersonKrr getCustomPersonKrr(String personIdentifier, String reserved, String status, String alertStatus, Kontaktinformasjon contactInfo) {
        return new PersonKrr(
                personIdentifier,
                reserved,
                status,
                alertStatus,
                contactInfo
        );
    }

    public static Kontaktinformasjon getValidKontaktinformasjon() {
        return new Kontaktinformasjon(
                getTestEmailAddress(),
                getTestPhoneNumber()
        );
    }

    public static Subject getValidSubject() {
        return new Subject(getValidSyntheticPersonIdentifier());
    }
}
