package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NnidClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata = new DocumentMetadata(
            List.of(
                    new DocumentMetadata.Display("no", "Norsk identitetsnummer"),
                    new DocumentMetadata.Display("en", "Norwegian identification number")
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
    public Map<String, Object> push(PreAuthorizedIssuanceContext issuanceContext, Map<String, String> claims) {
        return new HashMap<>(claims);
    }

}
