package no.idporten.eudiw.connector.authoritativesources.freg;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceDataNotFoundException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceInvalidDataException;
import org.springframework.stereotype.Service;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.FREG;

@Service
public class PidMdocCredentialDataSource implements CredentialDataSource {
    static final String CREDENTIAL_TYPE = "eu.europa.ec.eudi.pid.1";
    private final FregService fregService;
    private final PidService pidService;

    public PidMdocCredentialDataSource(FregService fregService, PidService pidService) {
        this.fregService = fregService;
        this.pidService = pidService;
    }


    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        PersonResource person = fregService.getEidasPerson(subject.identifier(), "EUDIW-ISSUER");
        validate(person);
        return pidService.createMdocPid(person, subject.identifier());
    }

    private void validate(PersonResource person) {
        if (person == null) {
            throw new AuthoritativeSourceDataNotFoundException(FREG, "Not found", "User not found in FREG");
        }
        if (person.getFoedselsdato() == null) {
            throw new AuthoritativeSourceInvalidDataException(FREG, "Missing data", "Foedselsdato is null");
        }
    }
}
