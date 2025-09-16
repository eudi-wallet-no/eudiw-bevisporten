package no.idporten.eudiw.issuer.claimssource.pid;

import com.nimbusds.jwt.JWT;
import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.Claim;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * A mock claims source generating test data.
 */
public class PIDClaimsSource implements ClaimsSource {

    public static final String NAMESPACE = "eu.europa.ec.eudi.pid.1";
    private ClaimsSourceProperties properties;

    private final FregService fregService;
    private final PersonConverterService personConverterService;


    public PIDClaimsSource(FregService fregService, PersonConverterService personConverterService) {
        this.fregService = fregService;
        this.personConverterService = personConverterService;
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

    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {

        // TODO read pid rule book.
        //  https://eu-digital-identity-wallet.github.io/eudi-doc-architecture-and-reference-framework/latest/annexes/annex-3/annex-3.01-pid-rulebook/#2-pid-attributes-and-metadata
        String fnr;
        try {
            fnr = accessToken.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Failed to extract fnr/dnr from access token", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        PersonResource person = fregService.getEidasPerson(fnr, "EUDIW-ISSUER");

        if(person == null || person.getNavn() == null) {
            // todo error handling
            throw new IssuerServerException("invalid_request", "User not found in FREG", HttpStatus.BAD_REQUEST);
        }

        List<Claim> claims = new ArrayList<>();
        // mandatory attributes
        claims.add(Claim.builder().path(NAMESPACE).path("personal_administrative_number").value(fnr).build());
        claims.add(Claim.builder().path(NAMESPACE).path("family_name").value(person.getNavn().getEtternavn()).build());
        claims.add(Claim.builder().path(NAMESPACE).path("given_name").value(person.getNavn().getFornavn()).build());
        claims.add(Claim.builder().path(NAMESPACE).path("birth_date").value(person.getFoedselsdato()).build());
        claims.add(Claim.builder().path(NAMESPACE).path("birth_place").value(person.getFoedested()).build());
        claims.add(Claim.builder().path(NAMESPACE).path("nationality").value(personConverterService.getFirstNationalityAlpha2(person.getStatsborgerskap())).build());
        // optional pid rulebook attributes
        boolean over18 = personConverterService.calcAgeOver18(person.getFoedselsdato());
        claims.add(Claim.builder().path(NAMESPACE).path("age_over_18").value(Boolean.valueOf(over18).toString()).build());
        // mandatory metadata
        claims.add(Claim.builder().path(NAMESPACE).path("expiry_date").value(personConverterService.calcPidExpiryDate()).build());
        claims.add(Claim.builder().path(NAMESPACE).path("issuing_authority").value("NO").build());
        claims.add(Claim.builder().path(NAMESPACE).path("issuing_country").value("NO").build());
        return claims;
    }



}
