package no.idporten.eudiw.connector.authoritativesources.freg;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceInvalidDataException;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.FREG;

@Service
public class AgeVerificationAuthoritativeSource implements AuthoritativeSource {
    private final FregService fregService;
    private final PersonConverterService personConverterService;

    public AgeVerificationAuthoritativeSource(FregService fregService, PersonConverterService personConverterService) {
        this.fregService = fregService;
        this.personConverterService = personConverterService;
    }


    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        PersonResource person = fregService.getEidasPerson(subject.identifier(), "EUDIW-ISSUER");
        validate(person);
        return buildAgeVerificationCredentialData(person, List.of(15, 18));
    }

    @Override
    public String getSource() {
        return "ageverification";
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

    private static void validate(PersonResource person) {
        if (person == null || person.getFoedselsdato() == null) {
            throw new ClaimsSourceInvalidDataException(FREG, "User not found in FREG");
        }
    }
}
