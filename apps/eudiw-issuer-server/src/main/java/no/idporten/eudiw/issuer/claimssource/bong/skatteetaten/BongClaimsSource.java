package no.idporten.eudiw.issuer.claimssource.bong.skatteetaten;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BongClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata = new DocumentMetadata(
            Map.of(
                    "no", "Drikkebong"
            ),
            List.of(
                    new ClaimMetadata("eining",
                            Map.of(
                                    "no", "Enhet"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$"),
                    new ClaimMetadata("nytar",
                            Map.of(
                                    "no", "Nytar"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$"),
                    new ClaimMetadata("betalar",
                            Map.of(
                                    "no", "Betalar"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$"),
                    new ClaimMetadata("age_over_18",
                            Map.of(
                                    "no", "Over 18 år"),
                            true,
                            "^true|false$")


            )
    );

    @Override
    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @Override
    public Map<String, String> push(String issuanceTransactionId, JWT accessToken, Map<String, String> claims) {
        return claims;
    }

}
