package no.idporten.eudiw.issuer.authoritativesources;

import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.claimssource.CredentialMetadataContext;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.credentials.types.ClaimMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JUnitClaimsSource implements ClaimsSource {

    private final DocumentMetadata documentMetadata;

    public JUnitClaimsSource(){
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Junit doc", "Kun for junit-tester")),
                List.of(new ClaimMetadata("junitdoc", "attr1", ClaimDataType.STRING, Map.of("no", "attribute1"), true, ".*"))
        );
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        return documentMetadata;
    }

    @Override
    public List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
        return List.of();
    }
}
