package no.idporten.eudiw.issuer.claimssource.pid;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.domain.PersonnavnResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.claimssource.AbstractAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


/**
 * Claims source for Norwegian PID data from FREG
 */
@Service
public class PIDClaimsSource extends AbstractAuthorizedClaimsSource {

    private final FregService fregService;
    private final PersonConverterService personConverterService;

    private final DocumentMetadata documentMetadata;


    public PIDClaimsSource(FregService fregService, PersonConverterService personConverterService) {
        this.fregService = fregService;
        this.personConverterService = personConverterService;
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Norsk ID-bevis")),
                List.of(
                        new ClaimMetadata("personal_administrative_number",
                                Map.of("no", "Fødselsnummer"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata("given_name",
                                Map.of("no", "Førenamn"),
                                true,
                                null),
                        new ClaimMetadata("family_name",
                                Map.of("no", "Etternamn"),
                                true,
                                null),
                        new ClaimMetadata("birth_date",
                                Map.of("no", "Fødselsdato"),
                                true,
                                null),
                        new ClaimMetadata("place_of_birth",
                                Map.of("no", "Fødeland"),
                                true,
                                null),
                        new ClaimMetadata("nationality",
                                Map.of("no", "Nasjonalitet"),
                                true,
                                null),
                        new ClaimMetadata("expiry_date",
                                Map.of("no", "Gyldig til dato"),
                                true,
                                null),
                        new ClaimMetadata("issuing_authority",
                                Map.of("no", "Utsteda av"),
                                true,
                                null),
                        new ClaimMetadata("issuing_country",
                                Map.of("no", "Utsteda i land"),
                                true,
                                null)

                )
        );
    }

    @Override
    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @Override
    public List<Claim> pull(String personIdentifier) {

        PersonResource person = fregService.getEidasPerson(personIdentifier, "EUDIW-ISSUER");

        if (person == null || person.getNavn() == null) {
            throw new ClaimsSourceInvalidDataException(AuthoritativeSource.FREG.name(), "User not found in FREG");
        }

        List<Claim> claims = new ArrayList<>();
        // mandatory attributes
        claims.add(getStringClaim("personal_administrative_number", personIdentifier));
        claims.add(getStringClaim("family_name", getEtternavn(person.getNavn())));
        claims.add(getStringClaim("given_name", getFornavn(person.getNavn())));
        claims.add(getFullDateClaim("birth_date", person.getFoedselsdato()));
        claims.add(getMapClaim("place_of_birth", convertBirthPlace(person)));
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
