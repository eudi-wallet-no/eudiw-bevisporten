package no.idporten.eudiw.issuer.claimssource.pid;

import com.nimbusds.jwt.JWT;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.claimssource.Claim;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * A mock claims source generating test data.
 */
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
        // mandatory attributes
        claims.add(Claim.builder().path(NAMESPACE).path("personal_administrative_number").value(accessToken.getJWTClaimsSet().getSubject()).build());
        claims.add(Claim.builder().path(NAMESPACE).path("family_name").value("FANTERI").build());
        claims.add(Claim.builder().path(NAMESPACE).path("given_name").value("JUKS ÅGE").build());
        claims.add(Claim.builder().path(NAMESPACE).path("birth_date").value("1999-01-01").build());
        claims.add(Claim.builder().path(NAMESPACE).path("birth_place").value("NO").build());
        claims.add(Claim.builder().path(NAMESPACE).path("nationality").value("NO").build());
        // optional pid rulebook attributes
        claims.add(Claim.builder().path(NAMESPACE).path("age_over_18").value("true").build());
        // mandatory metadata
        claims.add(Claim.builder().path(NAMESPACE).path("expiry_date").value(calcPidExpiryDate()).build());
        claims.add(Claim.builder().path(NAMESPACE).path("issuing_authority").value("NO").build());
        claims.add(Claim.builder().path(NAMESPACE).path("issuing_country").value("NO").build());
        return claims;
    }

    private String calcPidExpiryDate() {
        return LocalDate.now().plusDays(90).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

}
