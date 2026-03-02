package no.idporten.eudiw.issuer.claimssource;


import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.credentials.types.*;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("When using pre-authorized claims sources")
public class PreAuthorizedClaimsSourceTest {

    abstract static class AbstractJUnitClaimsSource extends AbstractPreAuthorizedClaimsSource {

        public AbstractJUnitClaimsSource() {
            setClaimsSourceCache(new InMemoryClaimsSourceCache());
        }
    }

    JWT maskinportenToken() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().build();
        return new PlainJWT(claimsSet);
    }

    JWT authProxyToken(String issuanceTransactionId) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().claim("tx_id", issuanceTransactionId).build();
        return new PlainJWT(claimsSet);
    }

    ExtendedCredentialMetadata extendedCredentialMetadata() {
        return new ExtendedCredentialMetadata(
                List.of(new Display("no", "Junit doc", "Kun for junit-tester")),
                List.of(new ExtendedClaimsDescription(null, "c", ClaimDataType.STRING, Map.of("no", "c1"), true, ".*"))
        );
    }

    @DisplayName("When using pull-based claims source")
    @Nested
    class PullTests {

        static class PullClaimsSource extends AbstractJUnitClaimsSource {
            @Override
            public CredentialData pull(PreAuthorizedIssuanceContext preAuthorizedIssuanceContext) {
                return new CredentialData(Map.of("c", "v"), null);
            }
        }

        @DisplayName("then push is not supported")
        @Test
        void pushNotSupported() {
            PreAuthorizedClaimsSource claimsSource = new PullClaimsSource();
            IssuerServerException e = assertThrows(IssuerServerException.class, () -> claimsSource.preAuthorize(
                    new PreAuthorizedIssuanceContext(new IssuanceTransactionId(), maskinportenToken()),
                    new CredentialData(Map.of("some", "data"), "ccid"))
            );
            assertTrue(e.getMessage().contains("does not support push"));
        }

        @DisplayName("then pre-authorized claims are pulled, validated and stored")
        @Test
        public void testPullClaimsSourceLifecycle() {
            final IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
            PreAuthorizedClaimsSource claimsSource = spy(new PullClaimsSource());
            claimsSource.preAuthorize(
                    new PreAuthorizedIssuanceContext(issuanceTransactionId, extendedCredentialMetadata(), "ccid", maskinportenToken(), Duration.ofMinutes(9)),
                    null);
            List<Claim> claims = claimsSource.issueClaims(new CredentialIssueContext(authProxyToken(issuanceTransactionId.getValue()), extendedCredentialMetadata()));
            assertAll(
                    () -> assertEquals(1, claims.size()),
                    () -> assertEquals("c", claims.getFirst().getPath().getFirst()),
                    () -> assertEquals("v", ((StringValue) claims.getFirst().getValue()).value())
            );
            verify(claimsSource).pull(any());
            verify(claimsSource).validate(any(), any(CredentialData.class));
            verify(claimsSource).store(eq(issuanceTransactionId), any(), eq(Duration.ofMinutes(9)));
            verify(claimsSource, never()).push(any(), any());
        }

    }

    @Nested
    class PushTests {

        static class PushClaimsSource extends AbstractJUnitClaimsSource {

            @Override
            public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
                return credentialData;
            }

        }

        @DisplayName("then pull is not supported")
        @Test
        void pullNotSupported() {
            PreAuthorizedClaimsSource claimsSource = new PushClaimsSource();
            IssuerServerException e = assertThrows(IssuerServerException.class, () -> claimsSource.preAuthorize(
                    new PreAuthorizedIssuanceContext(new IssuanceTransactionId(), maskinportenToken()), null));
            assertTrue(e.getMessage().contains("does not support pull"));
        }

        @DisplayName("then pre-authorized claims are pushed, validated and stored")
        @Test
        public void testPushClaimsSourceLifecycle() {
            final IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
            PreAuthorizedClaimsSource claimsSource = spy(new PushClaimsSource());
            claimsSource.preAuthorize(
                    new PreAuthorizedIssuanceContext(issuanceTransactionId, extendedCredentialMetadata(), "ccid", maskinportenToken(), Duration.ofMinutes(5)),
                    new CredentialData(Map.of("c", "v"), "ccid"));
            List<Claim> claims = claimsSource.issueClaims(new CredentialIssueContext(authProxyToken(issuanceTransactionId.getValue()), extendedCredentialMetadata()));
            assertAll(
                    () -> assertEquals(1, claims.size()),
                    () -> assertEquals("c", claims.getFirst().getPath().getFirst()),
                    () -> assertEquals("v", ((StringValue) claims.getFirst().getValue()).value())
            );
            verify(claimsSource).push(any(), any());
            verify(claimsSource).validate(any(), any(CredentialData.class));
            verify(claimsSource).store(eq(issuanceTransactionId), any(), eq(Duration.ofMinutes(5)));
            verify(claimsSource, never()).pull(any());
        }

    }

}
