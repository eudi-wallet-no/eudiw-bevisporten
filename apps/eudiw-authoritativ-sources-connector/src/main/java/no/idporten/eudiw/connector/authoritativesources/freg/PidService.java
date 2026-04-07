package no.idporten.eudiw.connector.authoritativesources.freg;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.domain.PersonnavnResource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceInvalidDataException;
import no.idporten.eudiw.connector.authoritativesources.freg.pidfields.MdocFieldNames;
import no.idporten.eudiw.connector.authoritativesources.freg.pidfields.PidFieldNames;
import no.idporten.eudiw.connector.authoritativesources.freg.pidfields.SdJwtFieldNames;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.FREG;

@Service
public class PidService {
    private final PersonConverterService personConverterService;

    public PidService(PersonConverterService personConverterService) {
        this.personConverterService = personConverterService;
    }

    public CredentialData createMdocPid(PersonResource person, String identifier) {
        return createPid(person, identifier, MdocFieldNames.getInstance());
    }

    public CredentialData createSdJwtPid(PersonResource person, String identifier) {
       return createPid(person, identifier, SdJwtFieldNames.getInstance());
    }

    private CredentialData createPid(PersonResource person, String identifier, PidFieldNames fieldNames) {
        CredentialData credentialData = new CredentialData();

        credentialData.addString(fieldNames.getPersonalAdministrativeNumber(), identifier);
        credentialData.addString(fieldNames.getFamilyName(), getFamilyName(person.getNavn()));
        credentialData.addString(fieldNames.getGivenName(), getGivenAndMiddleName(person.getNavn()));
        credentialData.addString(fieldNames.getBirthDate(), getBirthDate(person));
        credentialData.addStringMap(fieldNames.getPlaceOfBirth(), convertBirthPlace(person));
        credentialData.addStringList(fieldNames.getNationality(), getNationalities(person));

        credentialData.addString(fieldNames.getExpiryDate(), createPidExpiryDate().toString());
        credentialData.addString(fieldNames.getIssuingAuthority(), "DIGITALISERINGSDIREKTORATET");
        credentialData.addString(fieldNames.getIssuingCountry(), "NO");

        return credentialData;
    }


    private String getFamilyName(PersonnavnResource name) {
        return personConverterService.truncateTo150(name.getEtternavn());
    }

    // Attributes in FREG can be 200 chars long, but PID spec says 150 max, must truncate names. Does not apply to the other attributes used here.
    private String getGivenAndMiddleName(PersonnavnResource name) {
        String givenAndMiddleName = name.getMellomnavn() != null ? name.getFornavn() + " " + name.getMellomnavn() : name.getFornavn();
        return personConverterService.truncateTo150(givenAndMiddleName);
    }

    private static String getBirthDate(PersonResource person) {
        String birthDate = person.getFoedselsdato();
        if (birthDate == null || birthDate.isEmpty()) {
            throw new AuthoritativeSourceInvalidDataException(FREG, "Found no 'foedselsdato' in FREG for user");
        }
        return birthDate;
    }

    private LocalDate createPidExpiryDate() {
        return personConverterService.calcPidExpiryDate();
    }

    private List<String> getNationalities(PersonResource person) {
        if (person.getStatsborgerskap() == null) {
            throw new AuthoritativeSourceInvalidDataException(FREG,"Found no 'statsborgerskap' in FREG on user");
        }
        return personConverterService.getNationalitiesAlpha2(person.getStatsborgerskap());
    }

    private Map<String, String> convertBirthPlace(PersonResource person) {
        String country = personConverterService.getNationalityAlpha2(person.getFoedested());
        return Collections.singletonMap("country", country);
    }

}
