package no.idporten.eudiw.issuer.authoritativesources.pid;

import no.digdir.freg.domain.PersonResource;
import no.digdir.freg.domain.PersonnavnResource;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.claimssource.AbstractAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.credentials.ClaimValueConverter;
import no.idporten.eudiw.issuer.credentials.types.Claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.FREG;
import static no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription.EMPTY_NAMESPACE;


/**
 * Claims source for Norwegian PID data from FREG.  Subclasses handles format specific claim formats.
 * See https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md.
 */
class PIDClaimsSource extends AbstractAuthorizedClaimsSource {

    private final FregService fregService;
    private final PersonConverterService personConverterService;

    private final ClaimValueConverter claimValueConverter = new ClaimValueConverter();

    public PIDClaimsSource(FregService fregService, PersonConverterService personConverterService) {
        this.fregService = fregService;
        this.personConverterService = personConverterService;
    }

    public List<Claim> issueClaims(CredentialIssueContext credentialIssueContext){
        String personIdentifier = credentialIssueContext.personIdentifier();
        PersonResource person = fregService.getEidasPerson(personIdentifier, "EUDIW-ISSUER");

        if (person == null || person.getNavn() == null) {
            throw new ClaimsSourceInvalidDataException(FREG.name(), "User not found in FREG");
        }

        List<Claim> claims = new ArrayList<>();
        // mandatory attributes
        claims.add(claimValueConverter.getStringClaim(calculatePath("personal_administrative_number"), personIdentifier));
        claims.add(claimValueConverter.getStringClaim(calculatePath("family_name"), getEtternavn(person.getNavn())));
        claims.add(claimValueConverter.getStringClaim(calculatePath("given_name"), getFornavn(person.getNavn())));
        claims.add(claimValueConverter.fullDateClaim(calculatePath("birth_date"), getFoedselsdato(person)));
        claims.add(claimValueConverter.getMapClaim(calculatePath("place_of_birth"), convertBirthPlace(person)));
        claims.add(claimValueConverter.getListClaim(calculatePath("nationality"), getNationalities(person)));

        // mandatory metadata attributes
        claims.add(claimValueConverter.fullDateClaim(calculatePath("expiry_date"), createPidExpiryDate()));
        claims.add(claimValueConverter.getStringClaim(calculatePath("issuing_authority"), "DIGITALISERINGSDIREKTORATET"));
        claims.add(claimValueConverter.getStringClaim(calculatePath("issuing_country"), "NO"));

        return claims;
    }

    protected List<String> calculatePath(String dataIdentifier) {
        String namespace = getNamespace();
        List<String> path = new ArrayList<>();
        if (namespace != null) {
            path.add(namespace);
        }
        path.add(calculateAttributeIdentifier(dataIdentifier));
        return path;
    }

    private static String getFoedselsdato(PersonResource person) {
        String foedselsdato = person.getFoedselsdato();
        if (foedselsdato == null || foedselsdato.isEmpty()) {
            throw new ClaimsSourceInvalidDataException(FREG.name(), "Found no %s in FREG for user".formatted(foedselsdato));
        }
        return foedselsdato;
    }

    private LocalDate createPidExpiryDate() {
        return personConverterService.calcPidExpiryDate();
    }

    private List<String> getNationalities(PersonResource person) {
        if (person.getStatsborgerskap() == null) {
            throw new ClaimsSourceInvalidDataException(FREG.name(),"Found no Statsborgerskap in FREG on user");
        }
        return personConverterService.getNationalitiesAlpha2(person.getStatsborgerskap());
    }

    private Map<String, String> convertBirthPlace(PersonResource person) {
        String country = personConverterService.getNationalityAlpha2(person.getFoedested());
        return Collections.singletonMap("country", country);
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

    @Override
    public String getAuthorativeSourceName(){
        return FREG.name();
    }

    /**
     * Gets format-specific attribute identifier.  Default is the data identifier itself.
     */
    protected String calculateAttributeIdentifier(String dataIdentifier) {
        return dataIdentifier;
    }

    /**
     * Gets namespace - only relevant for mdoc.
     * @return namespace
     */
    protected String getNamespace() {
        return EMPTY_NAMESPACE;
    }

}
