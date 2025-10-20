package no.idporten.eudiw.issuer.claimssource.krr;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.krr.model.PersonKrr;
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
                Map.of("no", "Digital kontaktinformasjon"),
                List.of(
                        new ClaimMetadata("personidentifikator",
                                Map.of("no", "Personidentifikator"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata("epostadresse",
                                Map.of("no", "Epost"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("mobiltelefonnummer",
                                Map.of("no", "Telefonnummer"),
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
        PersonKrr personPrivate = krrIntegration.retrieve(personIdentifier);
        Map<String, String> claims = new HashMap<>();
        claims.put("personidentifikator", personIdentifier);
        claims.put("epostadresse", personPrivate.kontaktinformasjon().epostadresse());
        claims.put("mobiltelefonnummer", personPrivate.kontaktinformasjon().mobiltelefonnummer());
        claims.values().removeIf(Objects::isNull);
        return claims;
    }

}
