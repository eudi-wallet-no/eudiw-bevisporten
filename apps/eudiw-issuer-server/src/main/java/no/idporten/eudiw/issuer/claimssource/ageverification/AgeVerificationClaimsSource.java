package no.idporten.eudiw.issuer.claimssource.ageverification;

import com.nimbusds.jwt.JWT;
import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.claimssource.pid.PersonConverterService;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Claims source for Norwegian Age verification data from FREG
 */
@Service
public class AgeVerificationClaimsSource implements ClaimsSource {

    private ClaimsSourceProperties properties;

    private final FregService fregService;
    private final PersonConverterService personConverterService;


    public AgeVerificationClaimsSource(FregService fregService, PersonConverterService personConverterService) {
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
                .display(Display.builder().locale("no").name("Aldersbevis").build())
                .claim(ClaimsDescription.builder()
                        .path("age_over_18")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Over 18 år").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("age_over_15")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Over 15 år").build()).build())
                .build();
    }

    @Override
    public List<Claim> retrieveClaims(JWT accessToken) {

        String fnr;
        try {
            fnr = accessToken.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Failed to extract fnr/dnr from access token", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        PersonResource person = fregService.getEidasPerson(fnr, "EUDIW-ISSUER");

        if (person == null || person.getFoedselsdato() == null) {
            throw new ClaimsSourceInvalidDataException(AuthoritativeSource.FREG.name(), "User not found in FREG");
        }

        List<Claim> claims = new ArrayList<>();
        // mandatory attributes
        boolean over18 = personConverterService.calcAgeOver(person.getFoedselsdato(), 18);
        claims.add(getBooleanClaim("age_over_18", over18));
        boolean over15 = personConverterService.calcAgeOver(person.getFoedselsdato(), 15);
        claims.add(getBooleanClaim("age_over_15", over15));

        // mandatory metadata attributes
        claims.add(getDateTimeClaim("expiry_date", personConverterService.calcPidExpiryDate()));
        claims.add(getStringClaim("issuing_authority", "DIGITALISERINGSDIREKTORATET"));
        claims.add(getStringClaim("issuing_country", "NO"));

        return claims;
    }

    private Claim getDateTimeClaim(String key, ZonedDateTime value) {
        return buildClaim(key, new DateTimeValue(value));
    }

    private static Claim buildClaim(String key, ClaimValue claimValue) {
        return Claim.builder().path(key).value(claimValue).build();
    }

    private Claim getStringClaim(String key, String value) {
        return buildClaim(key, new StringValue(value));
    }
    private Claim getBooleanClaim(String key, Boolean value) {
        return buildClaim(key, new BooleanValue(value));
    }


}
