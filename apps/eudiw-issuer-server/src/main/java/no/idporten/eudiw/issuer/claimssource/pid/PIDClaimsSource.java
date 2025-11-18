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
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceProperties;
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
                .display(Display.builder().locale("no").name("Norsk ID-bevis").build())
                .claim(ClaimsDescription.builder()
                        .path("personal_administrative_number")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Fødselsnummer").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("given_name")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Førenamn").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("family_name")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Etternamn").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("birth_date")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Fødselsdato").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("place_of_birth")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Fødeland").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("nationality")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Nasjonalitet").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("expiry_date")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Gyldig til dato").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("issuing_authority")
                        .mandatory(true)
                        .display(Display.builder().locale("no").name("Utsteda av").build()).build())
                .claim(ClaimsDescription.builder()
                        .path("issuing_country")
                        .mandatory(true)
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
        claims.add(buildClaim("expiry_date", new FullDateValue(personConverterService.calcPidExpiryDate())));
        claims.add(getStringClaim("issuing_authority", "DIGITALISERINGSDIREKTORATET"));
        claims.add(getStringClaim("issuing_country", "NO"));

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

    public Claim getFullDateClaim(String key, String value) {
        if (value == null || value.isEmpty()) {
            throw new ClaimsSourceInvalidDataException(AuthoritativeSource.FREG.name(), "Found no %s in FREG on user".formatted(key));
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
        return Claim.builder().path(key).value(claimValue).build();
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
