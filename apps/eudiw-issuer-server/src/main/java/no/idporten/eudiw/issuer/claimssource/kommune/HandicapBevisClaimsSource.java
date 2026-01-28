package no.idporten.eudiw.issuer.claimssource.kommune;

import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class HandicapBevisClaimsSource extends AbstractPreAuthorizedClaimsSource {

    public final String NAMESPACE = "no:kommunalsektor:hcregister:1";

    private final DocumentMetadata documentMetadata;

    public HandicapBevisClaimsSource() {
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Handicapbevis", "Handicapbevis utstedt av kommune (Hackathon Brukerrådet 2025)")),
                List.of(
                        new ClaimMetadata(NAMESPACE,
                                "fodselsnummer",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Fødselsnummer"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata(NAMESPACE,
                                "bevis_nummer",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Bevisnummer"),
                                true,
                                "^\\d{1,20}$"),
                        new ClaimMetadata(NAMESPACE,
                                "kommune_nummer",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Kommunenummer"),
                                true,
                                "^\\d{1,20}$"),
                        new ClaimMetadata(NAMESPACE,
                                "status",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Status"),
                                false,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,50}$"),
                        new ClaimMetadata(NAMESPACE,
                                "bruker_kode",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Brukerkode"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,50}$"),
                        new ClaimMetadata(NAMESPACE,
                                "type",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Type"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,50}$"),
                        new ClaimMetadata(NAMESPACE,
                                "gyldig_fra",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Gyldig fra"),
                                true,
                                "^\\d{4}-\\d{2}-\\d{2}$"),
                        new ClaimMetadata(NAMESPACE,
                                "gyldig_til",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Gyldig til"),
                                true,
                                "^\\d{4}-\\d{2}-\\d{2}$")
                )
        );
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        return documentMetadata;
    }

    @Override
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        return credentialData;
    }

}
