package no.idporten.eudiw.issuer.claimssource.vegvesenet;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.*;

// Dummy førerkort implementation, not for production use!
@Service
public class ForerkortClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private static final String FORERKORT_DISCLAIMER_NO =
        "MERK: dette er ikkje eit reelt førarkort kobla til test-førarkortregisteret. Alle testbrukarar får statisk satt klasse B.";
    private static final String FORERKORT_DISCLAIMER_EN =
        "NOTE: this is not a real driver's license tied to the test license register. All test users are granted class B license.";

    private final DocumentMetadata documentMetadata = new DocumentMetadata(
            List.of(
                new DocumentMetadata.Display("no", "Norsk førerkort", FORERKORT_DISCLAIMER_NO),
                new DocumentMetadata.Display("en", "Norwegian driver's license", FORERKORT_DISCLAIMER_EN)
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
                    new ClaimMetadata("birth_date", TYPE_FULLDATE,
                            Map.of(
                                    "no", "Fødselsdato",
                                    "en", "Date of birth"),
                            true,
                            "^\\d{4}-\\d{2}-\\d{2}$"),
                    new ClaimMetadata("issue_date", TYPE_FULLDATE,
                            Map.of(
                                    "no", "Gyldig fra dato",
                                    "en", "Issue date"),
                            true,
                            "^\\d{4}-\\d{2}-\\d{2}$"),
                    new ClaimMetadata("expiry_date", TYPE_FULLDATE,
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
                    new ClaimMetadata("document_number",TYPE_NUMBER,
                            Map.of(
                                    "no", "Førerkort nummer",
                                    "en", "Document number"),
                            true,
                            "^\\d{1,150}$"),
                    new ClaimMetadata("portrait", TYPE_BINARY,
                            Map.of(
                                    "no", "Portrett bilde",
                                    "en", "Portrait Photos"),
                            false,
                            "^[-A-Za-z0-9+/]*={0,3}$"), // base64 regex
                    new ClaimMetadata("driving_privileges", TYPE_MAP,
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
    public final IssuanceTransactionId store(IssuanceTransactionId issuanceTransactionId, final Map<String, Object> claims, Duration lifetime) {
        Map<String, Object> claimsMapStringified = new HashMap<>(claims);
        return super.store(issuanceTransactionId, claimsMapStringified, lifetime);
    }


    @Override
    public Map<String, Object> push(IssuanceTransactionId issuanceTransactionId, JWT accessToken, Map<String, String> claims) {
        Map<String, Object> completeClaims = new HashMap<>(claims);
        // convert input birth_date to LocalDate
        if (claims.containsKey("birth_date")) {
            String birthDateStr = claims.get("birth_date");
            LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            completeClaims.put("birth_date", birthDate);
        }

        // Add our "hard coded" claim
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nowDays = utcNow.truncatedTo(ChronoUnit.DAYS);
        completeClaims.put("issue_date", nowDays.toLocalDate());
        completeClaims.put("expiry_date", nowDays.plusYears(1).toLocalDate());
        completeClaims.put("issuing_country", "NO");
        completeClaims.put("issuing_authority", "Statens vegvesen");
        completeClaims.put("document_number", 123456789);
        Map<String, Object> drivingPrivilege = new HashMap<>();
        drivingPrivilege.put("vehicle_category_code", "B");
        drivingPrivilege.put("issue_date", nowDays.toLocalDate());
        drivingPrivilege.put("expiry_date", nowDays.plusYears(1).toLocalDate());
        drivingPrivilege.put("codes", "96"); // https://www.vegvesen.no/globalassets/forerkort/har-forerkort/koder-pa-forerkort.pdf // hengar BE-96 :-)
        completeClaims.put("driving_privileges", drivingPrivilege);
        completeClaims.put("un_distinguishing_sign", "N");

        return completeClaims;
    }

    @Override
    public Claim getClaim(ClaimMetadata claim, Map<String, Object> storedClaims) {

        if (claim.type().equals(TYPE_BINARY)) {
            String base64Image = (String) storedClaims.get(claim.name());
            byte[] imageAsBytes = Base64.getDecoder().decode(base64Image);
            return Claim.builder().path(claim.name()).value(new BinaryValue(imageAsBytes)).build();
        } else if (claim.type().equals(TYPE_MAP) && claim.name().equals("driving_privileges")) {
            // special handling of driving_privileges claim for now
            Map<String, Object> dp = (Map<String, Object>) storedClaims.get(claim.name());
            Map<String, ClaimValue> dpClaims = new HashMap<>();
            dpClaims.put("vehicle_category_code", new StringValue((String) dp.get("vehicle_category_code")));
            dpClaims.put("issue_date", new FullDateValue(LocalDate.parse((String) dp.get("issue_date"))));
            dpClaims.put("expiry_date", new FullDateValue(LocalDate.parse((String) dp.get("expiry_date"))));
            dpClaims.put("codes", new StringValue((String) dp.get("codes")));
            return Claim.builder().path(claim.name()).value(new MapValue(dpClaims)).build();
        } else if (claim.type().equals(TYPE_FULLDATE)) {
            return Claim.builder().path(claim.name()).value(new FullDateValue(LocalDate.parse((String) storedClaims.get(claim.name())))).build();
        } else if (claim.type().equals(TYPE_NUMBER)) {
            return Claim.builder().path(claim.name()).value(new NumberValue((Integer) storedClaims.get(claim.name()))).build();
        }
        return super.getClaim(claim, storedClaims);
    }


}
