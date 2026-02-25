package no.idporten.eudiw.issuer.authoritativesources;

import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.credentials.types.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.types.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JUnitClaimsSource implements ClaimsSource {

    private final ExtendedCredentialMetadata extendedCredentialMetadata;

    public JUnitClaimsSource(){
        this.extendedCredentialMetadata = new ExtendedCredentialMetadata(
                List.of(new Display("no", "Junit doc", "Kun for junit-tester")),
                List.of(new ExtendedClaimsDescription("junitdoc", "attr1", ClaimDataType.STRING, Map.of("no", "attribute1"), true, ".*"))
        );
    }
    @Override
    public List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
        return List.of();
    }
}
