package no.idporten.eudiw.issuer.claimssource.advokattilsynet;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdvokatregisteretClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata;

    public AdvokatregisteretClaimsSource() {
        this.documentMetadata = new DocumentMetadata(
                Map.of("no", "Advokatbevilling"),
                List.of(
                        new ClaimMetadata("tittel",
                                Map.of("no", "Tittel"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("etternavn",
                                Map.of("no", "Etternavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("fornavn",
                                Map.of("no", "Fornavn"),
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
        // Dette er en gave til vår jurist - det fjernes så snart vi har ekte data
        if ("63879500925".equals(accessToken.getJWTClaimsSet().getStringClaim("pid"))) {
            return Map.of("tittel", "Frilynt advokat",
                    "etternavn", "Hagelin",
                    "fornavn", "Hans Gunnar");
        }
        return Map.of("tittel", "Advokat",
                "etternavn", "Tastad",
                "fornavn", "Hans");
    }

}
