package no.idporten.eudiw.issuer.claimssource;


import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("When using pre-authorized claims sources")
public class PreAuthorizedClaimsSourceTest {

    abstract class AbstractJUnitClaimsSource extends AbstractPreAuthorizedClaimsSource {
        @Override
        protected DocumentMetadata getDocumentMetadata() {
            return new DocumentMetadata(Map.of("no", "Junit"), List.of(new ClaimMetadata("c", Map.of("no", "C"), true, ".*")));
        }
    }

    ClaimsSourceProperties claimsSourceProperties() {
        ClaimsSourceProperties claimsSourceProperties = new ClaimsSourceProperties();
        claimsSourceProperties.setDoctype("junit.1");
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
            public Map<String, String> pull(String issuanceTransactionId, JWT accessToken) {
                return Map.of("c", "v");
            }
        }

        @DisplayName("then push is not supported")
        @Test
        void pushNotSupported() {
            PreAuthorizedClaimsSource claimsSource = new PullClaimsSource();
            claimsSource.init(claimsSourceProperties());
            IssuerServerException e = assertThrows(IssuerServerException.class, () -> claimsSource.preAuthorize("tx", maskinportenToken(), Map.of("some", "data")));
            assertTrue(e.getMessage().contains("does not support push"));
        }

        @DisplayName("then pre-authorized claims are pulled, validated and stored")
        @Test
        public void testPullClaimsSourceLifecycle() {
            PreAuthorizedClaimsSource claimsSource = spy(new PullClaimsSource());
            claimsSource.init(claimsSourceProperties());
            claimsSource.preAuthorize("tx", maskinportenToken(), null);
            List<Claim> claims = claimsSource.retrieveClaims(authProxyToken("tx"));
            assertAll(
                    () -> assertEquals(1, claims.size()),
                    () -> assertEquals("junit.1", claims.getFirst().getPath().getFirst()),
                    () -> assertEquals("c", claims.getFirst().getPath().getLast()),
                    () -> assertEquals("v", ((StringValue)claims.getFirst().getValue()).value())
            );
            verify(claimsSource).pull(eq("tx"), any());
            verify(claimsSource).validate(any());
            verify(claimsSource).store(eq("tx"), any());
            verify(claimsSource, never()).push(any(), any(), any());
        }

    }

    @Nested
    class PushTests {

        class PushClaimsSource extends AbstractJUnitClaimsSource {

            @Override
            public Map<String, String> push(String issuanceTransactionId, JWT accessToken, Map<String, String> claims) {
                return claims;
            }

        }

        @DisplayName("then pull is not supported")
        @Test
        void pullNotSupported() {
            PreAuthorizedClaimsSource claimsSource = new PushClaimsSource();
            claimsSource.init(claimsSourceProperties());
            IssuerServerException e = assertThrows(IssuerServerException.class, () -> claimsSource.preAuthorize("tx", maskinportenToken(), null));
            assertTrue(e.getMessage().contains("does not support pull"));
        }

        @DisplayName("then pre-authorized claims are pushed, validated and stored")
        @Test
        public void testPushClaimsSourceLifecycle() {
            PreAuthorizedClaimsSource claimsSource = spy(new PushClaimsSource());
            claimsSource.init(claimsSourceProperties());
            claimsSource.preAuthorize("tx", maskinportenToken(), Map.of("c", "v"));
            List<Claim> claims = claimsSource.retrieveClaims(authProxyToken("tx"));
            assertAll(
                    () -> assertEquals(1, claims.size()),
                    () -> assertEquals("junit.1", claims.getFirst().getPath().getFirst()),
                    () -> assertEquals("c", claims.getFirst().getPath().getLast()),
                    () -> assertEquals("v", ((StringValue)claims.getFirst().getValue()).value())
            );
            verify(claimsSource).push(eq("tx"), any(), any());
            verify(claimsSource).validate(any());
            verify(claimsSource).store(eq("tx"), any());
            verify(claimsSource, never()).pull(any(), any());
        }

    }

}
