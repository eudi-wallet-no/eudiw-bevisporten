package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.DocumentMetadata;

import java.util.List;
import java.util.Map;

public class NnidClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata = new DocumentMetadata(
            Map.of(
                    "no", "Norsk identitetsnummer",
                    "en", "Norwegian identification number"
            ),
            List.of(
                    new ClaimMetadata("norwegian_national_id_number",
                            Map.of(
                                    "no", "Norsk identitetsnummer",
                                    "en", "Norwegian identification number"),
                            true,
                            "^\\d{11}$"),
                    new ClaimMetadata("norwegian_national_id_number_type",
                            Map.of(
                                    "no", "Type norsk identitetsnummer",
                                    "en", "Type of Norwegian identification number"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$")
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
