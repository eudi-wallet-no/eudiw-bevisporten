package no.idporten.eudiw.issuer.claimssource.pid;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.claimssource.Claim;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.ArrayList;
import java.util.List;

public class PIDClaimsSource implements ClaimsSource {

    public static final String NAMESPACE = "eu.europa.ec.eudi.pid.1";
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
                .display(Display.builder().name("Norwegian PID").build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("personal_administrative_number")
                        .display(Display.builder().name("Fnr/dnr").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("given_name")
                        .display(Display.builder().name("Given name").build()).build())
                .build();
    }

    @SneakyThrows
    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {
        List<Claim> claims = new ArrayList<>();
        claims.add(Claim.builder().path(NAMESPACE).path("personal_administrative_number").value(accessToken.getJWTClaimsSet().getSubject()).build());
        claims.add(Claim.builder().path(NAMESPACE).path("given_name").value("navn" + accessToken.getJWTClaimsSet().getSubject()).build());
        return claims;
    }

}
