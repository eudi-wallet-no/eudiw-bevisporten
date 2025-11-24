package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.AbstractAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.Inntekt;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.InntektsOpplysninger;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.Respons;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
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

    private final DocumentMetadata documentMetadata;

    public InntektClaimsSource(InntektsApiIntegration inntektsApiIntegration) {
        this.inntektsApiIntegration = inntektsApiIntegration;
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Inntektsbevis")),
                List.of(
                        new ClaimMetadata("fastlonn",
                                Map.of("no", "Fastlønn"),
                                true,
                                "^[\\x20-\\x7E]{1,200}$")));
    }

    @Override
    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    // TODO rename to pull
    @SneakyThrows
    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {
        String personIdentifier;
        try {
            personIdentifier = accessToken.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Failed to extract fnr/dnr from access token", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
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

}
