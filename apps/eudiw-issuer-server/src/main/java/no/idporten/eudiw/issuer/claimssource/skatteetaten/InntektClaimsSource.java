package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import no.idporten.eudiw.issuer.claimssource.AbstractAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimDataType;
import no.idporten.eudiw.issuer.claimssource.CredentialMetadataContext;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.Inntekt;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.InntektsOpplysninger;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.Respons;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.EMPTY_NAMESPACE;


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
                        new ClaimMetadata(
                                EMPTY_NAMESPACE,
                                "fastlonn",
                                ClaimDataType.MAP,
                                Map.of("no", "Fastlønn"),
                                true,
                                "^[\\x20-\\x7E]{1,200}$")));
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        return documentMetadata;
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
