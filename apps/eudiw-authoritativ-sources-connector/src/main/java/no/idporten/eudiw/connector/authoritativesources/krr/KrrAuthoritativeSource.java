package no.idporten.eudiw.connector.authoritativesources.krr;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.connector.authoritativesources.krr.model.PersonKrr;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.KRR;
import static org.springframework.util.StringUtils.hasText;

@Service
public class KrrAuthoritativeSource implements AuthoritativeSource {

    private final KrrIntegration krrIntegration;

    public KrrAuthoritativeSource(KrrIntegration krrIntegration) {
        this.krrIntegration = krrIntegration;
    }

    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        PersonKrr personPrivate = krrIntegration.retrieve(subject.identifier());
        validate(personPrivate, subject);
        return createCredentialData(personPrivate);
    }

    @Override
    public String getSource() {
        return "krr";
    }

    private void validate(PersonKrr personKrr, Subject subject) {
        if (personKrr == null) {
            throw new ClaimsSourceDataNotFoundException(KRR, "No data available", "Failed to map response");
        }
        if (!Objects.equals(personKrr.personidentifikator(), subject.identifier())) {
           throw new ClaimsSourceInvalidDataException(KRR, "invalid_request", "The request is not valid", "Wrong person returned from KRR");
        }
        if (!Objects.equals(personKrr.reservasjon(), "NEI")) {
            throw new ClaimsSourceInvalidDataException(KRR, "invalid_request", "The request is not valid", "Person RESERVED in KRR");
        }
        if (!Objects.equals(personKrr.status(), "AKTIV")) {
            throw new ClaimsSourceInvalidDataException(KRR, "invalid_request", "The request is not valid", "Person not ACTIVE in KRR");
        }
        if (!Objects.equals(personKrr.varslingsstatus(), "KAN_VARSLES")) {
            throw new ClaimsSourceInvalidDataException(KRR, "invalid_request", "The request is not valid", "Person not verified in KRR");
        }
        if (!hasText(personKrr.kontaktinformasjon().epostadresse()) &&
                !hasText(personKrr.kontaktinformasjon().mobiltelefonnummer())) {
            throw new ClaimsSourceInvalidDataException(KRR, "invalid_request", "The request is not valid", "Person has neither epost nor mobil in KRR");
        }
    }

    private CredentialData createCredentialData(PersonKrr personPrivate) {
        CredentialData credentialData = new CredentialData();
        credentialData.put("personidentifikator", personPrivate.personidentifikator());
        credentialData.put("epostadresse", personPrivate.kontaktinformasjon().epostadresse());
        credentialData.put("mobiltelefonnummer", personPrivate.kontaktinformasjon().mobiltelefonnummer());
        credentialData.values().removeIf(Objects::isNull);
        return credentialData;
    }
}
