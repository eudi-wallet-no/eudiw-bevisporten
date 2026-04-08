package no.idporten.eudiw.connector.authoritativesources.freg;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceDataNotFoundException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceInvalidDataException;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.FREG;

@Service
public class AgeVerificationCredentialDataSource implements CredentialDataSource {
    static final String CREDENTIAL_TYPE = "eu.europa.ec.av.1";
    private final FregService fregService;
    private final PersonConverterService personConverterService;

    public AgeVerificationCredentialDataSource(FregService fregService, PersonConverterService personConverterService) {
        this.fregService = fregService;
        this.personConverterService = personConverterService;
    }


    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        PersonResource person = fregService.getEidasPerson(subject.identifier(), "EUDIW-ISSUER");
        validate(person);
        return buildAgeVerificationCredentialData(person, List.of(15, 18));
    }

    private CredentialData buildAgeVerificationCredentialData(PersonResource person, List<Integer> ages) {
        CredentialData credentialData = new CredentialData();

        for (Integer age : ages) {
            boolean overAge = personConverterService.calcAgeOver(person.getFoedselsdato(), age);
            String overAgeKey = "age_over_" + age;
            credentialData.addBoolean(overAgeKey, overAge);
        }

        return credentialData;
    }

    private void validate(PersonResource person) {
        if (person == null) {
            throw new AuthoritativeSourceDataNotFoundException(FREG, "User not found in FREG");
        }
        if (person.getFoedselsdato() == null) {
            throw new AuthoritativeSourceInvalidDataException(FREG, "Missing birthdate for user in FREG");
        }
    }
}
