package no.idporten.eudiw.issuer.claimssource.advokattilsynet;

import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.claimssource.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimDataType;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AdvokatregisteretClaimsSource extends AbstractPreAuthorizedClaimsSource {

    public static final String NAMESPACE = "no.advokattilsynet.advokatregisteret.1";

    private final DocumentMetadata documentMetadata;
    private final AdvokatregisteretIntegration advokatregisteretIntegration;

    public AdvokatregisteretClaimsSource(AdvokatregisteretIntegration advokatregisteretIntegration) {
        this.advokatregisteretIntegration = advokatregisteretIntegration;
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Advokatbevilling")),
                List.of(
                        new ClaimMetadata(NAMESPACE,
                                "personidentifikator",
                                ClaimDataType.STRING,
                                Map.of("no", "Personidentifikator"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata(NAMESPACE,
                                "tittel",
                                ClaimDataType.STRING,
                                Map.of("no", "Tittel"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata(NAMESPACE,
                                "mellomnavn",
                                ClaimDataType.STRING,
                                Map.of("no", "Mellomnavn"),
                                false,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata(NAMESPACE,
                                "etternavn",
                                ClaimDataType.STRING,
                                Map.of("no", "Etternavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata(NAMESPACE,
                                "fornavn",
                                ClaimDataType.STRING,
                                Map.of("no", "Fornavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata(NAMESPACE,
                                "regnr",
                                ClaimDataType.STRING,
                                Map.of("no", "Regnr"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$")
                )
        );
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        return documentMetadata;
    }

    @SneakyThrows
    @Override
    public CredentialData pull(PreAuthorizedIssuanceContext preAuthorizedIssuanceContext) {
        String personIdentifier = preAuthorizedIssuanceContext.accessToken().getJWTClaimsSet().getStringClaim("pid");
        PersonPrivate personPrivate = advokatregisteretIntegration.retrieve(personIdentifier);
        Map<String, Object> claims = new HashMap<>();
        claims.put("personidentifikator", personIdentifier);
        claims.put("tittel", personPrivate.tittel());
        claims.put("etternavn", personPrivate.etternavn());
        claims.put("fornavn", personPrivate.fornavn());
        claims.put("mellomnavn", personPrivate.mellomnavn());
        claims.put("regnr", personPrivate.regnr());
        claims.values().removeIf(Objects::isNull);
        return new CredentialData(claims, null);
    }

    @Override
    public String getAuthorativeSourceName() {
        return AuthoritativeSource.ADVOKATREGISTERET.name();
    }

}
