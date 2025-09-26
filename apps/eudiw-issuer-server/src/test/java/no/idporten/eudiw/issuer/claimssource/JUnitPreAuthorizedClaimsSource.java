package no.idporten.eudiw.issuer.claimssource;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JUnitPreAuthorizedClaimsSource extends AbstractPreAuthorizedClaimsSource {

    @Override
    protected DocumentMetadata getDocumentMetadata() {
        return new DocumentMetadata(Map.of("no", "Junit doc"), List.of(new ClaimMetadata("attr1", Map.of("no", "Attributt 1"), true, ".*")));
    }

}
