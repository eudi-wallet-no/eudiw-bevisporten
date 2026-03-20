package no.idporten.eudiw.issuer.authoritativesources.vegvesenet;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.types.*;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

// Dummy førerkort implementation, not for production use!
@Service
public class ForerkortClaimsSource extends AbstractPreAuthorizedClaimsSource {

    public static final String NAMESPACE = "org.iso.18013.5.1";
    
    private static final String FORERKORT_DISCLAIMER_NO =
        "MERK: dette er ikkje eit reelt førarkort kobla til test-førarkortregisteret. Alle testbrukarar får statisk satt klasse B.";
    private static final String FORERKORT_DISCLAIMER_EN =
        "NOTE: this is not a real driver's license tied to the test license register. All test users are granted class B license.";

    @Override
    public final IssuanceTransactionId store(PreAuthorizedIssuanceContext issuanceContext, final Map<String, Object> claims, Duration lifetime) {
        Map<String, Object> claimsMapStringified = new HashMap<>(claims);
        return super.store(issuanceContext, claimsMapStringified, lifetime);
    }


    @Override
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        Map<String, Object> completeClaims = new HashMap<>(credentialData.claims());
        // convert input birth_date to LocalDate
        if (completeClaims.containsKey("birth_date")) {
            String birthDateStr = (String)completeClaims.get("birth_date");
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

        return new CredentialData(completeClaims);
    }

    @Override
    public Claim getClaim(ExtendedClaimsDescription claim, Map<String, Object> storedClaims) {
       if (claim.type().equals(ClaimDataType.MAP) && claim.name().equals("driving_privileges")) {
            // special handling of driving_privileges claim for now
            Map<String, Object> dp = (Map<String, Object>) storedClaims.get(claim.name());
            Map<String, ClaimValue> dpClaims = new HashMap<>();
            dpClaims.put("vehicle_category_code", new StringValue((String) dp.get("vehicle_category_code")));
            dpClaims.put("issue_date", new FullDateValue(LocalDate.parse((String) dp.get("issue_date"))));
            dpClaims.put("expiry_date", new FullDateValue(LocalDate.parse((String) dp.get("expiry_date"))));
            dpClaims.put("codes", new StringValue((String) dp.get("codes")));
            return Claim.builder().path(NAMESPACE).path(claim.name()).value(new MapValue(dpClaims)).build();
        }
        return super.getClaim(claim, storedClaims);
    }

}
