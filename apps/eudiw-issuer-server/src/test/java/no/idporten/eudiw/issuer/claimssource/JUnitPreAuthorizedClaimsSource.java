package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JUnitPreAuthorizedClaimsSource extends AbstractPreAuthorizedClaimsSource {

    @Override
    protected DocumentMetadata getDocumentMetadata() {
        return new DocumentMetadata(List.of(new DocumentMetadata.Display("no", "Junit doc")), List.of(new ClaimMetadata("attr1", Map.of("no", "Attributt 1"), true, ".*")));
    }

}
