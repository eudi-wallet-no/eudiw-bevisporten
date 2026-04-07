package no.idporten.eudiw.connector.authoritativesources.freg;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceInvalidDataException;
import org.springframework.stereotype.Service;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.FREG;

@Service
public class PidSdJwtCredentialDataSource implements CredentialDataSource {
    static final String CREDENTIAL_TYPE = "urn:eudi:pid:1";
    private final FregService fregService;
    private final PidService pidService;

    public PidSdJwtCredentialDataSource(FregService fregService, PidService pidService) {
        this.fregService = fregService;
        this.pidService = pidService;
    }


    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        PersonResource person = fregService.getEidasPerson(subject.identifier(), "EUDIW-ISSUER");
        validate(person);
        return pidService.createSdJwtPid(person, subject.identifier());
    }

    private void validate(PersonResource person) {
        if (person == null || person.getFoedselsdato() == null) {
            throw new ClaimsSourceInvalidDataException(FREG, "User not found in FREG");
        }
    }
}
