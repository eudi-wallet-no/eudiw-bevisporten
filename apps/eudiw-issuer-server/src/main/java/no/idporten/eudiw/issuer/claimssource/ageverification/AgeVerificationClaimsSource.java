package no.idporten.eudiw.issuer.claimssource.ageverification;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.claimssource.AbstractAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.claimssource.pid.PersonConverterService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Claims source for Norwegian Age verification data from FREG
 */
@Service
public class AgeVerificationClaimsSource extends AbstractAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata;

    private final FregService fregService;
    private final PersonConverterService personConverterService;


    public AgeVerificationClaimsSource(FregService fregService, PersonConverterService personConverterService) {
        this.fregService = fregService;
        this.personConverterService = personConverterService;
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Aldersbevis")),
                List.of(
                        new ClaimMetadata("age_over_18",
                                Map.of("no", "Over 18 år"),
                                true,
                                "^true|false$"),
                        new ClaimMetadata("age_over_15",
                                Map.of("no", "Over 15 år"),
                                true,
                                "^true|false$")));
    }


    @Override
    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @Override
    public List<Claim> pull(String personIdentifier) {

        PersonResource person = fregService.getEidasPerson(personIdentifier, "EUDIW-ISSUER");

        if (person == null || person.getFoedselsdato() == null) {
            throw new ClaimsSourceInvalidDataException(AuthoritativeSource.FREG.name(), "User not found in FREG");
        }

        List<Claim> claims = new ArrayList<>();
        // mandatory attributes
        boolean over18 = personConverterService.calcAgeOver(person.getFoedselsdato(), 18);
        claims.add(getBooleanClaim("age_over_18", over18));
        boolean over15 = personConverterService.calcAgeOver(person.getFoedselsdato(), 15);
        claims.add(getBooleanClaim("age_over_15", over15));

//        // mandatory metadata attributes
//        claims.add(getDateTimeClaim("expiry_date", personConverterService.calcExpiryDateInMonths(1)));
//        claims.add(getStringClaim("issuing_authority", "DIGITALISERINGSDIREKTORATET"));
//        claims.add(getStringClaim("issuing_country", "NO"));

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
