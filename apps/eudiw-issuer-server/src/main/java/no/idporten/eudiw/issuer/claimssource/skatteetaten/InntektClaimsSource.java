package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimValue;
import no.idporten.eudiw.issuer.claimssource.domain.MapValue;
import no.idporten.eudiw.issuer.claimssource.domain.NumberValue;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * Claims source for Skatteetaten inntektsbevis.
 */
@Service
public class InntektClaimsSource implements ClaimsSource {

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
                .display(Display.builder().locale("no").name("Inntektsbevis").build())
                .claim(ClaimsDescription.builder()
                        .path("fastlonn")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Fastlønn").build()).build())
                .build();
    }

    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {
        Map<String, ClaimValue> fastlønnMap = Map.of(
                "2025-07", new NumberValue(38744),
                "2025-08", new NumberValue(38744)
        );
        return List.of(Claim.builder().path("fastlonn").value(new MapValue(fastlønnMap)).build());
    }

}
