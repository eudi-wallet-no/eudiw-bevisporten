package no.idporten.eudiw.issuer.claimssource.ageverification;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.claimssource.pid.PersonConverterService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Claims source for Norwegian Age verification data from FREG
 */
@Service
public class AgeVerificationClaimsSource extends AbstractAuthorizedClaimsSource {

    public final String NAMESPACE = "eu.europa.ec.av.1";

    private final DocumentMetadata documentMetadata;

    private final FregService fregService;
    private final PersonConverterService personConverterService;

    private final ClaimValueConverter claimValueConverter = new ClaimValueConverter();


    public AgeVerificationClaimsSource(FregService fregService, PersonConverterService personConverterService) {
        this.fregService = fregService;
        this.personConverterService = personConverterService;
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Aldersbevis")),
                List.of(
                        new ClaimMetadata(
                                NAMESPACE,
                                "age_over_18",
                                ClaimDataTypes.BOOLEAN,
                                Map.of("no",
                                        "Over 18 år"),
                                true,
                                "^true|false$"),
                        new ClaimMetadata(
                                NAMESPACE,
                                "age_over_15",
                                ClaimDataTypes.BOOLEAN,
                                Map.of("no", "Over 15 år"),
                                true,
                                "^true|false$")));
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
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
        claims.add(claimValueConverter.getBooleanClaim(List.of(NAMESPACE, "age_over_18"), over18));
        boolean over15 = personConverterService.calcAgeOver(person.getFoedselsdato(), 15);
        claims.add(claimValueConverter.getBooleanClaim(List.of(NAMESPACE, "age_over_15"), over15));

//        // mandatory metadata attributes
//        claims.add(getDateTimeClaim("expiry_date", personConverterService.calcExpiryDateInMonths(1)));
//        claims.add(getStringClaim("issuing_authority", "DIGITALISERINGSDIREKTORATET"));
//        claims.add(getStringClaim("issuing_country", "NO"));

        return claims;
    }

    @Override
    public String getAuthorativeSourceName() {
        return AuthoritativeSource.FREG.name();
    }
}
