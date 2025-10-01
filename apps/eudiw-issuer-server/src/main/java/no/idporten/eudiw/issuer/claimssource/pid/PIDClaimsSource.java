package no.idporten.eudiw.issuer.claimssource.pid;

import com.nimbusds.jwt.JWT;
import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.domain.PersonnavnResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


/**
 * Claims source for Norwegian PID data from FREG
 */
@Service
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
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Fødselsnummer").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("given_name")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Førenamn").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("family_name")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Etternamn").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("birth_date")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Fødselsdato").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("birth_place").path("country")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Fødeland").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("nationality")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Nasjonalitet").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("expiry_date")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Gyldig til dato").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("issuing_authority")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Utsteda av").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("issuing_country")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Utsteda i land").build()).build())
                .claim(ClaimsDescription.builder()
                        .path(NAMESPACE).path("age_over_18")
                        .mandatory(false)
                        .display(Display.builder().locale("no").name("Over 18").build()).build())
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
            throw new ClaimsSourceInvalidDataException(AuthoritativeSource.FREG.name(), "User not found in FREG");
        }

        List<Claim> claims = new ArrayList<>();
        // mandatory attributes
        claims.add(getStringClaim("personal_administrative_number", fnr));
        claims.add(getStringClaim("family_name", getEtternavn(person.getNavn())));
        claims.add(getStringClaim("given_name", getFornavn(person.getNavn())));
        claims.add(getFullDateClaim("birth_date", person.getFoedselsdato()));
        claims.add(getMapClaim("birth_place", convertBirthPlace(person)));
        claims.add(getListClaim("nationality", getNationalities(person)));

        // mandatory metadata attributes
        claims.add(getDateTimeClaim("expiry_date", personConverterService.calcPidExpiryDate()));
        claims.add(getStringClaim("issuing_authority", "DIGITALISERINGSDIREKTORATET"));
        claims.add(getStringClaim("issuing_country", "NO"));

        // digdir non-spec attributes
        boolean over18 = personConverterService.calcAgeOver18(person.getFoedselsdato());
        claims.add(getBooleanClaim("age_over_18", over18));

        return claims;
    }

    private List<String> getNationalities(PersonResource person) {
        if (person.getStatsborgerskap() == null) {
            throw new ClaimsSourceInvalidDataException(AuthoritativeSource.FREG.name(),"Found no Statsborgerskap in FREG on user");
        }
        return personConverterService.getNationalitiesAlpha2(person.getStatsborgerskap());
    }

    private Map<String, String> convertBirthPlace(PersonResource person) {
        String country = personConverterService.getNationalityAlpha2(person.getFoedested());
        return Collections.singletonMap("country", country);
    }

    private Claim getFullDateClaim(String key, String value) {
        if (value == null || value.isEmpty()) {
            throw new ClaimsSourceInvalidDataException(AuthoritativeSource.FREG.name(), "Found no Foedselsdato in FREG on user");
        }
        try {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return buildClaim(key, new FullDateValue(date));
        }catch(DateTimeParseException e){
            throw new ClaimsSourceInvalidDataException(AuthoritativeSource.FREG.name(), "Invalid Foedselsdato format in FREG on user");
        }
    }

    private Claim getDateTimeClaim(String key, ZonedDateTime value) {
        return buildClaim(key, new DateTimeValue(value));
    }

    private static Claim buildClaim(String key, ClaimValue claimValue) {
        return Claim.builder().path(NAMESPACE).path(key).value(claimValue).build();
    }

    private Claim getStringClaim(String key, String value) {
        return buildClaim(key, new StringValue(value));
    }
    private Claim getBooleanClaim(String key, Boolean value) {
        return buildClaim(key, new BooleanValue(value));
    }

    // Only support List of StringValue for now
    private Claim getListClaim(String key, List<String> value) {
        List<ClaimValue> list = new ArrayList<>();
        for (String v : value) {
            list.add(new StringValue(v));
        }
        return buildClaim(key, new ListValue(list));
    }

    // Only support Map of values of type StringValue for now
    private Claim getMapClaim(String key, Map<String, String> value) {
        Map<String, ClaimValue> map = new HashMap<>();
        for (String k : value.keySet()) {
            map.put(k, new StringValue(value.get(k)));
        }
        return buildClaim(key, new MapValue(map));
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
