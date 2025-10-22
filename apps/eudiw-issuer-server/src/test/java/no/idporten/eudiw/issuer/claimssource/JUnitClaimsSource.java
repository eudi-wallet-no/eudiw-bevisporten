package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JUnitClaimsSource implements ClaimsSource {

    private ClaimsSourceProperties properties;

    @Override
    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClaimsSourceProperties getProperties() {
        return properties;
    }

    @Override
    public ClaimsSourceMetadata getMetadata() {
        return ClaimsSourceMetadata.builder()
                .display(Display.builder().name("Junit doc").build())
                .claim(ClaimsDescription.builder()
                        .path("attr1")
                        .display(Display.builder().name("attribute1").build())
                        .build())
                .build();
    }

    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {
        return List.of();
    }
}
