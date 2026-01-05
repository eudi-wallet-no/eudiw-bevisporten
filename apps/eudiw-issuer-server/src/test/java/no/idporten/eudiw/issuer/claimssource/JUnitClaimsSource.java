package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JUnitClaimsSource implements ClaimsSource {

    private final DocumentMetadata documentMetadata;
    private ClaimsSourceProperties properties;

    public JUnitClaimsSource(){
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Junit doc", "Kun for junit-tester")),
                List.of(new ClaimMetadata("attr1", Map.of("no", "attribute1"), true, ".*"))
        );
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        return documentMetadata;
    }

    @Override
    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClaimsSourceProperties getProperties() {
        return properties;
    }

    @Override
    public List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
        return List.of();
    }
}
