package no.idporten.eudiw.issuer.authoritativesources.skatteetaten;

import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.credentials.types.ClaimMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NnidClaimsSource extends AbstractPreAuthorizedClaimsSource {

    public static final String NAMESPACE = "no.skatteetaten.nnid.1";

    private final DocumentMetadata documentMetadata = new DocumentMetadata(
            List.of(
                    new DocumentMetadata.Display("no", "Norsk identitetsnummer"),
                    new DocumentMetadata.Display("en", "Norwegian identification number")
            ),
            List.of(
                    new ClaimMetadata(NAMESPACE,
                            "norwegian_national_id_number",
                            ClaimDataType.STRING,
                            Map.of(
                                    "no", "Norsk identitetsnummer",
                                    "en", "Norwegian identification number"),
                            true,
                            "^\\d{11}$"),
                    new ClaimMetadata(NAMESPACE,
                            "norwegian_national_id_number_type",
                            ClaimDataType.STRING,
                            Map.of(
                                    "no", "Type norsk identitetsnummer",
                                    "en", "Type of Norwegian identification number"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$")
            )
    );

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        return documentMetadata;
    }

    @Override
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        return credentialData;
    }

}
