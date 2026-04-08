package no.idporten.eudiw.connector.authoritativesources.advokattilsynet;


import io.micrometer.common.util.StringUtils;
import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceDataNotFoundException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceInvalidDataException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.Objects;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.ADVOKATREGISTERET;
import static no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes.INVALID_REQUEST;

@Service
public class AdvokatregisteretCredentialDataSource implements CredentialDataSource {
    static final String CREDENTIAL_TYPE = "no.advokattilsynet.advokatregisteret.1";
    private final AdvokatregisteretIntegration advokatregisteretIntegration;

    public AdvokatregisteretCredentialDataSource(AdvokatregisteretIntegration advokatregisteretIntegration) {
        this.advokatregisteretIntegration = advokatregisteretIntegration;
    }

    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        String personIdentifier = subject.identifier();
        PersonPrivate personPrivate = advokatregisteretIntegration.retrieve(personIdentifier);
        validate(personPrivate);
        CredentialData claims = new CredentialData();
        claims.put("personidentifikator", personIdentifier);
        claims.put("tittel", personPrivate.tittel());
        claims.put("etternavn", personPrivate.etternavn());
        claims.put("fornavn", personPrivate.fornavn());
        claims.put("mellomnavn", personPrivate.mellomnavn());
        claims.put("regnr", personPrivate.regnr());
        claims.values().removeIf(Objects::isNull);
        return claims;
    }

    private void validate(PersonPrivate personPrivate) throws ResourceAccessException {
        if (personPrivate == null) {
            throw new AuthoritativeSourceDataNotFoundException(ADVOKATREGISTERET, "No data available", "Failed to map response");
        }
        if (StringUtils.isEmpty(personPrivate.tittel())) {
            throw new AuthoritativeSourceInvalidDataException(ADVOKATREGISTERET, INVALID_REQUEST, "No data available", "No value for title, assuming empty response");
        }
    }
}