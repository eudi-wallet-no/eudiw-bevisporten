package no.idporten.eudiw.issuer.claimssource.vegvesenet;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ForerkortClaimsSourceTest {

    @Test
    @DisplayName("when getDocumentMetadata is called, then DocumentMetadata with 11 claims is returned")
    void getDocumentMetadata() {
        ForerkortClaimsSource source = new ForerkortClaimsSource();
        DocumentMetadata documentMetadata = source.getDocumentMetadata(null);
        assertNotNull(documentMetadata);
        assertEquals(11, documentMetadata.claims().size());
        for(ClaimMetadata claimMetadata : documentMetadata.claims()) {
            assertEquals("org.iso.18013.5.1", claimMetadata.namespace());
            assertEquals(2, claimMetadata.path().size());
            assertEquals(claimMetadata.namespace(), claimMetadata.path().getFirst());
        }
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

        CredentialData credentialData = source.push(new PreAuthorizedIssuanceContext(txId, jwt), new CredentialData(Collections.unmodifiableMap(inputClaims), null));
        Map<String, Object> result = credentialData.claims();
        // Get all claim names from DocumentMetadata
        Set<String> expectedClaims = source.getDocumentMetadata(null).claims().stream()
                .map(ClaimMetadata::name)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(result.keySet().containsAll(expectedClaims), "All claims should be present in result");
    }
}