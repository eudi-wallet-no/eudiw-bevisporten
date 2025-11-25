package no.idporten.eudiw.issuer.claimssource.advokattilsynet;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.claimssource.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
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
                List.of(new DocumentMetadata.Display("no", "Advokatbevilling")),
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
    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @SneakyThrows
    @Override
    public Map<String, Object> pull(IssuanceTransactionId issuanceTransactionId, JWT accessToken) {
        String personIdentifier = accessToken.getJWTClaimsSet().getStringClaim("pid");
        PersonPrivate personPrivate = advokatregisteretIntegration.retrieve(personIdentifier);
        Map<String, Object> claims = new HashMap<>();
        claims.put("personidentifikator", personIdentifier);
        claims.put("tittel", personPrivate.tittel());
        claims.put("etternavn", personPrivate.etternavn());
        claims.put("fornavn", personPrivate.fornavn());
        claims.put("mellomnavn", personPrivate.mellomnavn());
        claims.put("regnr", personPrivate.regnr());
        claims.values().removeIf(Objects::isNull);
        return claims;
    }

    @Override
    public String getAuthorativeSourceName(){
        return AuthoritativeSource.ADVOKATREGISTERET.name();
    }

}
