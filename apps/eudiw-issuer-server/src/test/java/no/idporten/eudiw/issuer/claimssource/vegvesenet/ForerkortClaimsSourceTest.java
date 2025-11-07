package no.idporten.eudiw.issuer.claimssource.vegvesenet;

import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

class ForerkortClaimsSourceTest {

    @Test
    @DisplayName("when getDocumentMetadata is called, then DocumentMetadata with 11 claims is returned")
    void getDocumentMetadata() {
        ForerkortClaimsSource source = new ForerkortClaimsSource();
        DocumentMetadata documentMetadata = source.getDocumentMetadata();
        assertNotNull(documentMetadata);
        assertEquals(11, documentMetadata.claims().size());
    }

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

        Map<String, Object> result = source.push(txId, jwt, inputClaims);

        // Get all claim names from DocumentMetadata
        Set<String> expectedClaims = source.getDocumentMetadata().claims().stream()
                .map(ClaimMetadata::name)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(result.keySet().containsAll(expectedClaims), "All claims should be present in result");
    }
}