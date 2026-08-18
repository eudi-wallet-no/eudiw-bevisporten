package no.idporten.eudiw.issuer.claimssource.cache;

import no.idporten.eudiw.issuer.claimssource.InMemoryClaimsSourceCache;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static no.idporten.eudiw.issuer.TestData.junitIssuerTenant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("When using a cache to store credential data")
public class ClaimsSourceCacheTest {

    private ClaimsSourceCache claimsSourceCache = new InMemoryClaimsSourceCache();

    @DisplayName("then cache keys includes credential issuer tenant and transaction id")
    @Test
    void testCacheKeyGeneration() {
        assertEquals("memory:issuance:data:junit:123", claimsSourceCache.cacheKey("memory", junitIssuerTenant(), new IssuanceTransactionId("123")));
   }

   @DisplayName("then claims can be stored and retrieved, and are removed from cache after retrieval")
   @Test
    void testStoreAndRetrieveClaims() {
        Map<String, Object> claims = Map.of("claim1", "value1", "claim2", "value2");
        var transactionId = new IssuanceTransactionId("txn123");
        claimsSourceCache.storeClaims(junitIssuerTenant(), transactionId, claims, Duration.ofMinutes(5));
        Map<String, Object> retrievedClaims = claimsSourceCache.retrieveClaims(junitIssuerTenant(), transactionId);
        assertEquals(claims, retrievedClaims);
        assertNull(claimsSourceCache.retrieveClaims(junitIssuerTenant(), transactionId));
    }

}
