package no.idporten.eudiw.issuer.claimssource.vegvesenet;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Dummy førerkort implementation, not for production use!
@Service
public class ForerkortClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata = new DocumentMetadata(
            Map.of(
                    "no", "Norsk førerkort",
                    "en", "Norwegian driver's license"
            ),
            List.of(
                    new ClaimMetadata("family_name",
                            Map.of(
                                    "no", "Etternavn",
                                    "en", "Family name"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                    new ClaimMetadata("given_name",
                            Map.of(
                                    "no", "Fornavn",
                                    "en", "Given name"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                    new ClaimMetadata("birth_date",
                            Map.of(
                                    "no", "Fødselsdato",
                                    "en", "Date of birth"),
                            true,
                            "^\\d{4}-\\d{2}-\\d{2}$"),
                    new ClaimMetadata("issue_date",
                            Map.of(
                                    "no", "Gyldig fra dato",
                                    "en", "Issue date"),
                            true,
                            "^\\d{4}-\\d{2}-\\d{2}$"),
                    new ClaimMetadata("expiry_date",
                            Map.of(
                                    "no", "Gyldig til dato",
                                    "en", "Expiry date"),
                            true,
                            "^\\d{4}-\\d{2}-\\d{2}$"),
                    new ClaimMetadata("issuing_country",
                            Map.of(
                                    "no", "Utsted av land",
                                    "en", "Issuing country"),
                            true,
                            "^[A-Z]{2}$"),
                    new ClaimMetadata("issuing_authority",
                            Map.of(
                                    "no", "Utsteder myndighet",
                                    "en", "Issuing authority"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                    new ClaimMetadata("document_number",
                            Map.of(
                                    "no", "Førerkort nummer",
                                    "en", "Document number"),
                            true,
                            "^\\d{1,150}$"),
                    new ClaimMetadata("portrait",
                            Map.of(
                                    "no", "Portrett bilde",
                                    "en", "Portrait Photos"),
                            false,
                            "^[-A-Za-z0-9+/]*={0,3}$"), // base64 regex
                    new ClaimMetadata("driving_privileges",
                            Map.of(
                                    "no", "Førerkort klasser",
                                    "en", "Driving privileges"),
                            true,
                            "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                    new ClaimMetadata("un_distinguishing_sign",
                            Map.of(
                                    "no", "Land",
                                    "en", "Country"),
                            true,
                            "^[A-Z]{1,4}$")
            )
    );

    @Override
    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @Override
    public Map<String, String> push(IssuanceTransactionId issuanceTransactionId, JWT accessToken, Map<String, String> claims) {
        Map<String, String> completeClaims = new HashMap<>(claims);
        // add our "hard coded" claim
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nowDays = utcNow.truncatedTo(ChronoUnit.DAYS);
        completeClaims.put("issue_date", nowDays.format(DateTimeFormatter.ISO_LOCAL_DATE));
        completeClaims.put("expiry_date", nowDays.plusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        completeClaims.put("issuing_country", "NO");
        completeClaims.put("issuing_authority", "Statens vegvesen");
        completeClaims.put("document_number", "123456789");
        completeClaims.put("driving_privileges", "B"); // should be nested structure, but flat for now
        completeClaims.put("un_distinguishing_sign", "N");
        return completeClaims;
    }

}
