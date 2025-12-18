package no.idporten.eudiw.issuer.claimssource.kommune;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HandicapBevisClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata;

    public HandicapBevisClaimsSource() {
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Handicapbevis","Handicapbevis utstedt av kommune (Hackathon Brukerrådet 2025)")),
                List.of(
                        new ClaimMetadata("fodselsnummer",
                                Map.of("no", "Fødselsnummer"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata("bevis_nummer",
                                Map.of("no", "Bevisnummer"),
                                true,
                                "^\\d{1,20}$"),
                        new ClaimMetadata("kommune_nummer",
                                Map.of("no", "Kommunenummer"),
                                true,
                                "^\\d{1,20}$"),
                        new ClaimMetadata("status",
                                Map.of("no", "Status"),
                                false,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,50}$"),
                        new ClaimMetadata("bruker_kode",
                                Map.of("no", "Brukerkode"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,50}$"),
                        new ClaimMetadata("type",
                                Map.of("no", "Type"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,50}$"),
                        new ClaimMetadata("gyldig_fra",
                                Map.of("no", "Gyldig fra"),
                                true,
                                "^\\d{4}-\\d{2}-\\d{2}$"),
                        new ClaimMetadata("gyldig_til",
                                Map.of("no", "Gyldig til"),
                                true,
                                "^\\d{4}-\\d{2}-\\d{2}$")
                )
        );
    }

    @Override
    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @Override
    public Map<String, Object> push(PreAuthorizedIssuanceContext issuanceContext, Map<String, String> claims) {
        return new HashMap<>(claims);
    }

}
