package no.idporten.eudiw.issuer.authoritativesources.vegvesenet;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.claimssource.PushPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static no.idporten.eudiw.issuer.TestData.credentialConfigurationFromClasspath;
import static no.idporten.eudiw.issuer.TestData.junitIssuerTenant;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ForerkortTest {

    @Test
    @DisplayName("when call push with 4 claims, then all required claims from metadata are included in push result")
    void allRequiredClaimsFromMetadataAreIncludedInPushResult() {
        PreAuthorizedClaimsSource source = new PushPreAuthorizedClaimsSource();
        Map<String, Object> inputClaims = new HashMap<>();
        inputClaims.put("family_name", "Testesen");
        inputClaims.put("given_name", "Test");
        inputClaims.put("birth_date", "1990-01-01");
        inputClaims.put("portrait", "0");
        inputClaims.put("age_over_16", "true");
        inputClaims.put("age_over_18", "true");
        inputClaims.put("age_over_21", "true");
        inputClaims.put("age_in_years", "25");
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nowDays = utcNow.truncatedTo(ChronoUnit.DAYS);
        inputClaims.put("issue_date", nowDays.toLocalDate());
        inputClaims.put("expiry_date", nowDays.plusYears(1).toLocalDate());
        inputClaims.put("issuing_country", "NO");
        inputClaims.put("issuing_authority", "Statens vegvesen");
        inputClaims.put("document_number", 123456789);
        Map<String, Object> drivingPrivilege = new HashMap<>();
        drivingPrivilege.put("vehicle_category_code", "B");
        drivingPrivilege.put("issue_date", nowDays.toLocalDate());
        drivingPrivilege.put("expiry_date", nowDays.plusYears(1).toLocalDate());
        drivingPrivilege.put("codes", "96"); // https://www.vegvesen.no/globalassets/forerkort/har-forerkort/koder-pa-forerkort.pdf // hengar BE-96 :-)
        inputClaims.put("driving_privileges", drivingPrivilege);
        inputClaims.put("un_distinguishing_sign", "N");


        JWT jwt = mock(JWT.class);
        IssuanceTransactionId txId = new IssuanceTransactionId("tx-123");

        ExtendedCredentialConfiguration credentialConfiguration = credentialConfigurationFromClasspath("classpath:credential-configurations/vegvesenet/mdl_mso_mdoc.json");
        CredentialData credentialData = source.push(new PreAuthorizedIssuanceContext(junitIssuerTenant(), credentialConfiguration, txId, jwt), new CredentialData(Collections.unmodifiableMap(inputClaims)));
        Map<String, Object> result = credentialData.claims();
        ExtendedCredentialMetadata credentialMetadata =  credentialConfiguration.getExtendedCredentialMetadata();
        Set<String> expectedClaims = credentialMetadata.claims().stream()
                .map(ExtendedClaimsDescription::name)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(result.keySet().containsAll(expectedClaims), "All claims should be present in result");
    }
}