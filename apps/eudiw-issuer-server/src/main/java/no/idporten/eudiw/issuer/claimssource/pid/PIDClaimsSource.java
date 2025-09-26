package no.idporten.eudiw.issuer.claimssource.pid;

import com.nimbusds.jwt.JWT;
import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.domain.PersonnavnResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Claims source for Norwegian PID data from FREG
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
                .display(Display.builder().locale("no").name("Norsk PID").build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("personal_administrative_number")
                        .display(Display.builder().locale("no").name("Fødselsnummer").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("given_name")
                        .display(Display.builder().locale("no").name("Førenamn").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("family_name")
                        .display(Display.builder().locale("no").name("Etternamn").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("birth_date")
                        .display(Display.builder().locale("no").name("Fødselsdato").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("birth_place")
                        .display(Display.builder().locale("no").name("Fødeland").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("nationality")
                        .display(Display.builder().locale("no").name("Nasjonalitet").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("expiry_date")
                        .display(Display.builder().locale("no").name("Gyldig til dato").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("issuing_authority")
                        .display(Display.builder().locale("no").name("Utsteda av").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("issuing_country")
                        .display(Display.builder().locale("no").name("Utsteda i land").build()).build())
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

        if (person == null || person.getNavn() == null) {
            // todo error handling
            throw new IssuerServerException("invalid_request", "User not found in FREG", HttpStatus.BAD_REQUEST);
        }

        List<Claim> claims = new ArrayList<>();
        // mandatory attributes
        claims.add(getStringClaim("personal_administrative_number", fnr));
        claims.add(getStringClaim("family_name", getEtternavn(person.getNavn())));
        claims.add(getStringClaim("given_name", getFornavn(person.getNavn())));
        claims.add(getFullDateClaim("birth_date", person.getFoedselsdato()));
        claims.add(getStringClaim("birth_place", personConverterService.getNationalityAlpha2(person.getFoedested()))); // TODO: return map with country=value
        claims.add(getStringClaim("nationality", personConverterService.getFirstNationalityAlpha2(person.getStatsborgerskap()))); // TODO: return array of all nationalities

        // mandatory metadata attributes
        claims.add(getDateTimeClaim("expiry_date", personConverterService.calcPidExpiryDate()));
        claims.add(getStringClaim("issuing_authority", "DIGITALISERINGSDIREKTORATET"));
        claims.add(getStringClaim("issuing_country", "NO"));
        return claims;
    }

    private Claim getFullDateClaim(String key, String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        // TODO handle DateTimeParseException
        return Claim.builder().path(NAMESPACE).path(key).value(new FullDateValue(date)).build();
    }
    private Claim getDateTimeClaim(String key, ZonedDateTime value) {
        return Claim.builder().path(NAMESPACE).path(key).value(new DateTimeValue(value)).build();
    }
    private Claim getStringClaim(String key, String value) {
        return Claim.builder().path(NAMESPACE).path(key).value(new StringValue(value)).build();
    }

    // Attributes in FREG can be 200 chars long, but PID spec says 150 max, must truncate names. Does not apply to the other attributes used here.
    private String getEtternavn(PersonnavnResource navn) {
        return personConverterService.truncateTo150(navn.getEtternavn());
    }

    // Attributes in FREG can be 200 chars long, but PID spec says 150 max, must truncate names. Does not apply to the other attributes used here.
    private String getFornavn(PersonnavnResource navn) {
        String fornavn = navn.getMellomnavn() != null ? navn.getFornavn() + " " + navn.getMellomnavn() : navn.getFornavn();
        return personConverterService.truncateTo150(fornavn);
    }

}
