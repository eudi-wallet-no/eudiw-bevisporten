package no.idporten.eudiw.issuer.authoritativesources;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialMetadataContext;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.credentials.types.ClaimMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JUnitPreAuthorizedClaimsSource extends AbstractPreAuthorizedClaimsSource {

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        return new DocumentMetadata(List.of(new DocumentMetadata.Display("no", "Junit doc")), List.of(new ClaimMetadata("junitdoc", "attr1", ClaimDataType.STRING, Map.of("no", "Attributt 1"), true, ".*")));
    }

}
