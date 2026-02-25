package no.idporten.eudiw.issuer.authoritativesources.vegvesenet;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

class ForerkortClaimsSourceTest {


    @Disabled
    @Test
    @DisplayName("when call push with 4 claims, then all required claims from metadata are included in push result")
    void allRequiredClaimsFromMetadataAreIncludedInPushResult() {
        ForerkortClaimsSource source = new ForerkortClaimsSource();
        Map<String, String> inputClaims = new HashMap<>();
        inputClaims.put("family_name", "Testesen");
        inputClaims.put("given_name", "Test");
        inputClaims.put("birth_date", "1990-01-01");
        inputClaims.put("portrait", "0");

        JWT jwt = mock(JWT.class);
        IssuanceTransactionId txId = new IssuanceTransactionId("tx-123");

        CredentialData credentialData = source.push(new PreAuthorizedIssuanceContext(txId, jwt), new CredentialData(Collections.unmodifiableMap(inputClaims), null));
        Map<String, Object> result = credentialData.claims();
        // Get all claim names from DocumentMetadata
        // TODO
//        Set<String> expectedClaims = source.getDocumentMetadata(null).claims().stream()
//                .map(ExtendedClaimsDescription::name)
//                .collect(java.util.stream.Collectors.toSet());
//        assertTrue(result.keySet().containsAll(expectedClaims), "All claims should be present in result");
    }
}