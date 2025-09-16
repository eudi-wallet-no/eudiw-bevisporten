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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A mock claims source generating test data.
 */
public class PIDClaimsSource implements ClaimsSource {

    public static final String NAMESPACE = "eu.europa.ec.eudi.pid.1";
    private ClaimsSourceProperties properties;

    private final FregService fregService;

    // Convert ISO 3166-1 Alpha 3 to Alpha 2
    private final SortedMap<String, String> iso3166_1Alpha3ToAlpha2Map = new TreeMap<>();

    public PIDClaimsSource(FregService fregService) {
        this.fregService = fregService;
    }

    @Override
    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
        this.iso3166_1Alpha3ToAlpha2Map.putAll(createISO3661ConversionMap());
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

        // TODO integrate with FREG , les pid rule book.
        //  https://eu-digital-identity-wallet.github.io/eudi-doc-architecture-and-reference-framework/latest/annexes/annex-3/annex-3.01-pid-rulebook/#2-pid-attributes-and-metadata
        String fnr = null;
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
        claims.add(Claim.builder().path(NAMESPACE).path("nationality").value(getFirstNationalityAlpha2(person.getStatsborgerskap())).build());
        // optional pid rulebook attributes
        // TODO: use date of birth to calculate age_over_18
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

    private String getFirstNationalityAlpha2(List<String> nationalitiesAlpha3) {
        String nationality = nationalitiesAlpha3.getFirst();

        return iso3166_1Alpha3ToAlpha2Map.get(nationality);

    }


    // stolen from https://github.com/felleslosninger/idporten-c2id-server/blob/main/idporten-folkeregister-claims-source/src/main/java/no/idporten/c2id/server/spi/folkeregister/IDPortenFregClaimsSource.java#L87
    protected Map<String, String> createISO3661ConversionMap() {
        return Arrays.stream(Locale.getAvailableLocales())
                .filter(locale -> {
                    try {
                        return !locale.getISO3Country().isEmpty();
                    } catch (MissingResourceException e) {
                        return false;
                    }
                })
                .collect(Collectors.toMap(Locale::getISO3Country, Locale::getCountry, (key, duplicate) -> key));
    }

}
