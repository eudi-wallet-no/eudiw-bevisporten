package no.idporten.eudiw.issuer.claimssource;


import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.StringValue;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("When using pre-authorized claims sources")
public class PreAuthorizedClaimsSourceTest {

    abstract class AbstractJUnitClaimsSource extends AbstractPreAuthorizedClaimsSource {

        public AbstractJUnitClaimsSource() {
            setClaimsSourceCache(new InMemoryClaimsSourceCache());
        }

        @Override
        public DocumentMetadata getDocumentMetadata() {
            return new DocumentMetadata(List.of(new DocumentMetadata.Display("no", "Junit")), List.of(new ClaimMetadata("c", Map.of("no", "C"), true, ".*")));
        }
    }

    ClaimsSourceProperties claimsSourceProperties() {
        ClaimsSourceProperties claimsSourceProperties = new ClaimsSourceProperties();
        claimsSourceProperties.setCredentialTypes(Set.of("junit.1"));
        return claimsSourceProperties;
    }


    JWT maskinportenToken() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().build();
        return new PlainJWT(claimsSet);
    }

    JWT authProxyToken(String issuanceTransactionId) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().claim("tx_id", issuanceTransactionId).build();
        return new PlainJWT(claimsSet);
    }

    @DisplayName("When using pull-based claims source")
    @Nested
    class PullTests {

        class PullClaimsSource extends AbstractJUnitClaimsSource {
            @Override
            public Map<String, Object> pull(IssuanceTransactionId issuanceTransactionId, JWT accessToken) {
                return Map.of("c", "v");
            }
        }

        @DisplayName("then push is not supported")
        @Test
        void pushNotSupported() {
            PreAuthorizedClaimsSource claimsSource = new PullClaimsSource();
            claimsSource.init(claimsSourceProperties());
            IssuerServerException e = assertThrows(IssuerServerException.class, () -> claimsSource.preAuthorize(new IssuanceTransactionId(), maskinportenToken(), Map.of("some", "data"), Duration.ofMinutes(10)));
            assertTrue(e.getMessage().contains("does not support push"));
        }

        @DisplayName("then pre-authorized claims are pulled, validated and stored")
        @Test
        public void testPullClaimsSourceLifecycle() {
            final IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
            PreAuthorizedClaimsSource claimsSource = spy(new PullClaimsSource());
            claimsSource.init(claimsSourceProperties());
            claimsSource.preAuthorize(issuanceTransactionId, maskinportenToken(), null, Duration.ofMinutes(9));
            List<Claim> claims = claimsSource.issueClaims(authProxyToken(issuanceTransactionId.getValue()));
            assertAll(
                    () -> assertEquals(1, claims.size()),
                    () -> assertEquals("c", claims.getFirst().getPath().getFirst()),
                    () -> assertEquals("v", ((StringValue)claims.getFirst().getValue()).value())
            );
            verify(claimsSource).pull(eq(issuanceTransactionId), any());
            verify(claimsSource).validate(any());
            verify(claimsSource).store(eq(issuanceTransactionId), any(), eq(Duration.ofMinutes(9)));
            verify(claimsSource, never()).push(any(), any(), any());
        }

    }

    @Nested
    class PushTests {

        class PushClaimsSource extends AbstractJUnitClaimsSource {

            @Override
            public Map<String, Object> push(IssuanceTransactionId issuanceTransactionId, JWT accessToken, Map<String, String> claims) {
                return new HashMap<>(claims);
            }

        }

        @DisplayName("then pull is not supported")
        @Test
        void pullNotSupported() {
            PreAuthorizedClaimsSource claimsSource = new PushClaimsSource();
            claimsSource.init(claimsSourceProperties());
            IssuerServerException e = assertThrows(IssuerServerException.class, () -> claimsSource.preAuthorize(new IssuanceTransactionId(), maskinportenToken(), null, Duration.ofMinutes(10)));
            assertTrue(e.getMessage().contains("does not support pull"));
        }

        @DisplayName("then pre-authorized claims are pushed, validated and stored")
        @Test
        public void testPushClaimsSourceLifecycle() {
            final IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
            PreAuthorizedClaimsSource claimsSource = spy(new PushClaimsSource());
            claimsSource.init(claimsSourceProperties());
            claimsSource.preAuthorize(issuanceTransactionId, maskinportenToken(), Map.of("c", "v"), Duration.ofMinutes(5));
            List<Claim> claims = claimsSource.issueClaims(authProxyToken(issuanceTransactionId.getValue()));
            assertAll(
                    () -> assertEquals(1, claims.size()),
                    () -> assertEquals("c", claims.getFirst().getPath().getFirst()),
                    () -> assertEquals("v", ((StringValue)claims.getFirst().getValue()).value())
            );
            verify(claimsSource).push(eq(issuanceTransactionId), any(), any());
            verify(claimsSource).validate(any());
            verify(claimsSource).store(eq(issuanceTransactionId), any(), eq(Duration.ofMinutes(5)));
            verify(claimsSource, never()).pull(any(), any());
        }

    }

}
