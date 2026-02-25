package no.idporten.eudiw.issuer.authoritativesources.skatteetaten;

import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.issuer.authoritativesources.skatteetaten.domain.Inntekt;
import no.idporten.eudiw.issuer.authoritativesources.skatteetaten.domain.InntektsOpplysninger;
import no.idporten.eudiw.issuer.authoritativesources.skatteetaten.domain.Respons;
import no.idporten.eudiw.issuer.claimssource.AbstractAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.MapValue;
import no.idporten.eudiw.issuer.credentials.types.NumberValue;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


/**
 * Claims source for Skatteetaten inntektsbevis.
 */
@Service
public class InntektClaimsSource extends AbstractAuthorizedClaimsSource {

    private final InntektsApiIntegration inntektsApiIntegration;

    public InntektClaimsSource(InntektsApiIntegration inntektsApiIntegration) {
        this.inntektsApiIntegration = inntektsApiIntegration;
    }

    @Override
    public List<Claim> pull(String personIdentifier) {

        Respons respons = inntektsApiIntegration.retrieve(personIdentifier);

        Map<String, Double> fastlonnMap = new HashMap<>();
        for (InntektsOpplysninger inntektsOpplysninger : respons.oppgaveInntektsmottaker()) {
            if (!CollectionUtils.isEmpty(inntektsOpplysninger.inntekt())) {
                String maaned = inntektsOpplysninger.kalendermaaned();
                double sumFastlonn = 0d;
                for (Inntekt inntekt : inntektsOpplysninger.inntekt()) {
                    sumFastlonn += inntekt.beloep();
                }
                fastlonnMap.put(maaned, fastlonnMap.getOrDefault(maaned, 0d) + sumFastlonn);
            }
        }
        List<Claim> claims =  List.of(Claim.builder().path("fastlonn")
                .value(new MapValue(
                        new TreeMap<>(fastlonnMap
                                .entrySet().
                                stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> new NumberValue(e.getValue().longValue()))))))
                .build()
        );
        return  claims;
    }


    @Override
    public String getAuthorativeSourceName(){
        return AuthoritativeSource.INNTEKTSAPI.name();
    }

}
