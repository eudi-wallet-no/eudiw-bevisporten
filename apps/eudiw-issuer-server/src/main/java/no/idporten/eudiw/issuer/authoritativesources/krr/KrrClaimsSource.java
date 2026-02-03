package no.idporten.eudiw.issuer.authoritativesources.krr;

import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.credentials.types.ClaimMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import no.idporten.eudiw.issuer.authoritativesources.krr.model.PersonKrr;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class KrrClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata;
    private final KrrIntegration krrIntegration;

    public KrrClaimsSource(KrrIntegration krrIntegration) {
        this.krrIntegration = krrIntegration;
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Digital kontaktinformasjon")),
                List.of(
                        new ClaimMetadata("personidentifikator",
                                Map.of("no", "Personidentifikator"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata("epostadresse",
                                Map.of("no", "Epost"),
                                false,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("mobiltelefonnummer",
                                Map.of("no", "Telefonnummer"),
                                false,
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
    public CredentialData pull(PreAuthorizedIssuanceContext issuanceContext) {
        String personIdentifier = issuanceContext.accessToken().getJWTClaimsSet().getStringClaim("pid");
        PersonKrr personPrivate = krrIntegration.retrieve(personIdentifier);
        Map<String, Object> claims = new HashMap<>();
        claims.put("personidentifikator", personIdentifier);
        claims.put("epostadresse", personPrivate.kontaktinformasjon().epostadresse());
        claims.put("mobiltelefonnummer", personPrivate.kontaktinformasjon().mobiltelefonnummer());
        claims.values().removeIf(Objects::isNull);
        return new CredentialData(claims, null);
    }

    @Override
    public String getAuthorativeSourceName(){
        return AuthoritativeSource.KRR.name();
    }
}
