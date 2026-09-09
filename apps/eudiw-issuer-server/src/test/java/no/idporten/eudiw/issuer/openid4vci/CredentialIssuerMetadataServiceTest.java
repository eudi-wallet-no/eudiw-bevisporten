package no.idporten.eudiw.issuer.openid4vci;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("When creating credential issuer metadata")
@ActiveProfiles("junit")
@SpringBootTest
class CredentialIssuerMetadataServiceTest {

    private static final String ROOT_ISSUER = "https://junit.eidas2sandkasse.dev";

    @Autowired
    private CredentialIssuerMetadataService credentialIssuerMetadataService;

    @MockitoBean
    private AuditLogger auditLogger;

    @DisplayName("then signed metadata contains the required issuer claims and validity")
    @Test
    void testSignedMetadata() throws Exception {
        Instant beforeSigning = Instant.now();

        SignedJWT signedMetadata = SignedJWT.parse(
                credentialIssuerMetadataService.getSignedCredentialIssuerMetadata(null));
        JWTClaimsSet claims = signedMetadata.getJWTClaimsSet();

        assertAll(
                () -> assertFalse(signedMetadata.getSignature().toString().isBlank()),
                () -> assertEquals(ROOT_ISSUER, claims.getSubject()),
                () -> assertEquals(ROOT_ISSUER, claims.getIssuer()),
                () -> assertEquals(ROOT_ISSUER, claims.getStringClaim("credential_issuer")),
                () -> assertFalse(claims.getIssueTime().toInstant().isBefore(beforeSigning.minusSeconds(1))),
                () -> assertFalse(claims.getIssueTime().toInstant().isAfter(Instant.now().plusSeconds(1))),
                () -> assertEquals(
                        Duration.ofDays(1),
                        Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant())));
    }

    @DisplayName("then requesting signed metadata for a tenant without a signing keystore is rejected")
    @Test
    void testSignedMetadataWithoutSigningKeystore() {
        IssuerServerException exception = assertThrows(
                IssuerServerException.class,
                () -> credentialIssuerMetadataService.getSignedCredentialIssuerMetadata("junit"));

        assertAll(
                () -> assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus()),
                () -> assertEquals("invalid_request", exception.getError()));
    }
}
