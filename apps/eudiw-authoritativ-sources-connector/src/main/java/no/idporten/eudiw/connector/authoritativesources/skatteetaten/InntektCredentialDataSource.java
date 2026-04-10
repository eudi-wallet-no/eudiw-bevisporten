package no.idporten.eudiw.connector.authoritativesources.skatteetaten;


import no.idporten.eudiw.connector.authoritativesources.CredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceDataNotFoundException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceInvalidDataException;
import no.idporten.eudiw.connector.authoritativesources.skatteetaten.domain.InntektsOpplysninger;
import no.idporten.eudiw.connector.authoritativesources.skatteetaten.domain.Respons;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.SKATTEETATEN;


@Service
public class InntektCredentialDataSource implements CredentialDataSource {
    static final String CREDENTIAL_TYPE = "no.skatteetaten.inntekt.1";
    private final InntektsApiIntegration inntektsApiIntegration;

    public InntektCredentialDataSource(InntektsApiIntegration inntektsApiIntegration) {
        this.inntektsApiIntegration = inntektsApiIntegration;
    }

    @Override
    public CredentialData retrieveCredentialData(Subject subject) {
        Respons respons = getResponsForLast6Months(subject);
        validate(respons);

        Map<String, Long> fastlonnMap = buildFastlonnMap(respons);

        CredentialData credentialData = new CredentialData();
        credentialData.addNumberMap("fastlonn", fastlonnMap);
        return credentialData;
    }

    private Respons getResponsForLast6Months(Subject subject) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusMonths(6);
        return inntektsApiIntegration.retrieve(subject.identifier(), from, to);
    }

    private Map<String, Long> buildFastlonnMap(Respons respons) {
        return respons.oppgaveInntektsmottaker()
                .stream()
                .filter(inntektsOpplysninger -> !CollectionUtils.isEmpty(inntektsOpplysninger.inntekt()))
                .collect(Collectors.toMap(
                        InntektsOpplysninger::kalendermaaned,
                        InntektsOpplysninger::sumFastlonn,
                        Long::sum,
                        TreeMap::new
                ));
    }

    private void validate(Respons respons) {
        if (respons == null) {
            throw new AuthoritativeSourceDataNotFoundException(SKATTEETATEN, "No data available", "Failed to map response");
        }
        if (CollectionUtils.isEmpty(respons.oppgaveInntektsmottaker())) {
            throw new AuthoritativeSourceInvalidDataException(SKATTEETATEN, "Missing data", "Missing inntektsopplysninger for user in Skatteetaten");
        }
    }
}
