package no.idporten.eudiw.issuer.claimssource.advokattilsynet;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.advokattilsynet.model.PersonPrivate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AdvokatregisteretClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata;
    private final AdvokatregisteretIntegration advokatregisteretIntegration;

    public AdvokatregisteretClaimsSource(AdvokatregisteretIntegration advokatregisteretIntegration) {
        this.advokatregisteretIntegration = advokatregisteretIntegration;
        this.documentMetadata = new DocumentMetadata(
                Map.of("no", "Advokatbevilling"),
                List.of(
                        new ClaimMetadata("personidentifikator",
                                Map.of("no", "Personidentifikator"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata("tittel",
                                Map.of("no", "Tittel"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("mellomnavn",
                                Map.of("no", "Mellomnavn"),
                                false,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("etternavn",
                                Map.of("no", "Etternavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("fornavn",
                                Map.of("no", "Fornavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("regnr",
                                Map.of("no", "Regnr"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$")
                )
        );
    }

    @Override
    protected DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @SneakyThrows
    @Override
    public Map<String, String> pull(String issuanceTransactionId, JWT accessToken) {
        String personIdentifier = accessToken.getJWTClaimsSet().getStringClaim("pid");
        PersonPrivate personPrivate = advokatregisteretIntegration.retrieve(personIdentifier);
        Map<String, String> claims = new HashMap<>();
        claims.put("personidentifikator", personIdentifier);
        claims.put("tittel", personPrivate.tittel());
        claims.put("etternavn", personPrivate.etternavn());
        claims.put("fornavn", personPrivate.fornavn());
        claims.put("mellomnavn", personPrivate.mellomnavn());
        claims.put("regnr", personPrivate.regnr());
        claims.values().removeIf(Objects::isNull);
        return claims;
    }

}
