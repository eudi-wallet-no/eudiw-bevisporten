package no.idporten.eudiw.issuer.logging.audit;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.service.NotificationId;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@DisplayName("When audit logging is used")
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AuditService auditService;


    @DisplayName("then all audit entries are logged correctly for StartCredentialIssuanceTransaction")
    @Test
    void testLogStartCredentialIssuanceTransaction() throws ParseException {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);

        auditService.logStartCredentialIssuanceTransaction("issuer", "credentialConfigId", new IssuanceTransactionId("id"), createAccessToken("12345678901"));

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertEquals(AuditID.START_CREDENTIAL_ISSUANCE.auditIdentifier().auditId(), actualEntry.getAuditId().auditId());
        assertEquals("issuer", actualEntry.getAttributes().get(AuditService.CREDENTIAL_ISSUER));
        assertEquals("credentialConfigId", actualEntry.getAttributes().get(AuditService.CREDENTIAL_CONFIGURATION_ID));
        assertEquals("id", actualEntry.getAttributes().get(AuditService.ISSUANCE_TRANSACTION_ID));
        assertNotNull(actualEntry.getAttributes().get(AuditService.ACCESS_TOKEN));
    }

    @DisplayName("then all audit entries are logged correctly for CreateCredentialOffer")
    @Test
    void testLogCreateCredentialOffer() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);

        auditService.logCreateCredentialOffer("issuer", "credentialConfigId");

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertEquals(AuditID.CREATE_CREDENTIAL_OFFER.auditIdentifier().auditId(), actualEntry.getAuditId().auditId());
        assertEquals("issuer", actualEntry.getAttributes().get(AuditService.CREDENTIAL_ISSUER));
        assertEquals("credentialConfigId", actualEntry.getAttributes().get(AuditService.CREDENTIAL_CONFIGURATION_ID));
    }

    @DisplayName("then all audit entries are logged correctly for IssueCredentials")
    @Test
    void testLogIssueCredentials() throws ParseException {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        String authorizationServer = "authServer";
        String credentialConfigId = "credentialConfigId";
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId("33");
        String format = "jwt_vc";
        NotificationId notificationId = new NotificationId("222");
        JWT jwt = createAccessToken("12345678901");

        auditService.logIssueCredentials(authorizationServer, credentialConfigId, issuanceTransactionId, format, notificationId, jwt);

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertEquals(AuditID.ISSUE_CREDENTIAL.auditIdentifier().auditId(), actualEntry.getAuditId().auditId());
        assertEquals(authorizationServer, actualEntry.getAttributes().get(AuditService.AUTHORIZATION_SERVER));
        assertEquals(credentialConfigId, actualEntry.getAttributes().get(AuditService.CREDENTIAL_CONFIGURATION_ID));
        assertEquals(format, actualEntry.getAttributes().get(AuditService.FORMAT));
        assertEquals(notificationId, actualEntry.getAttributes().get(AuditService.NOTIFICATION_ID));
        assertEquals(issuanceTransactionId.getValue(), actualEntry.getAttributes().get(AuditService.ISSUANCE_TRANSACTION_ID));
        assertNotNull(actualEntry.getAttributes().get(AuditService.ACCESS_TOKEN));
    }

    @DisplayName("then all audit entries are logged correctly for WalletStatusUpdate")
    @Test
    void testLogWalletStatusUpdate() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        String credentialConfigId = "credentialConfigId";
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId("1");
        NotificationId notificationId = new NotificationId("444");
        String status = "COMPLETED";

        auditService.logWalletStatusUpdate(credentialConfigId, issuanceTransactionId, notificationId, status);

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertEquals(AuditID.WALLET_UPDATED_STATUS.auditIdentifier().auditId(), actualEntry.getAuditId().auditId());
        assertEquals(credentialConfigId, actualEntry.getAttributes().get(AuditService.CREDENTIAL_CONFIGURATION_ID));
        assertEquals(issuanceTransactionId.getValue(), actualEntry.getAttributes().get(AuditService.ISSUANCE_TRANSACTION_ID));
        assertEquals(notificationId, actualEntry.getAttributes().get(AuditService.NOTIFICATION_ID));
        assertEquals(status, actualEntry.getAttributes().get(AuditService.STATUS));
    }

    @DisplayName("then JWT masking works removes signature part")
    @Test
    void testMaskJwt() throws ParseException {
        JWT jwt = createAccessToken("12345678901");

        String tokenMasked = AuditService.maskJWT(jwt);

        assertNotNull(tokenMasked, "tokenMasked should not be null");
        String[] token = tokenMasked.split("\\.");
        assertEquals(2, token.length, "tokenMasked should contain 2 parts");

        String headerJson = new String(Base64.getUrlDecoder().decode(token[0]));
        assertNotNull(headerJson, "headerJson should not be null");
        assertFalse(headerJson.isEmpty(), "headerJson should not be empty");

        String body = new String(Base64.getUrlDecoder().decode(token[1]));
        assertNotNull(body, "body should not be null");
        assertFalse(body.isEmpty(), "body should not be empty");
        assertTrue(body.contains("12345678901"), "body should contain fnr");
    }

    @DisplayName("then JWT masking with invalid token throws exception")
    @Test
    void testMaskJwtForInvalidTokenThrowsException() {
        // Token with no signature part or header part (also invalid base64)
        JWT jwtWithOnlyBody = new PlainJWT(new JWTClaimsSet.Builder().claim("attr", "val").build());
        assertThrows(IssuerServerException.class, () -> AuditService.maskJWT(jwtWithOnlyBody));

        // Token with no signature part (also invalid base64)
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder().subject("33").build();
        JWSHeader header = new JWSHeader(new JWSAlgorithm(JWSAlgorithm.RS256.getName()));
        JWT jwtWithOutSignature = new SignedJWT(header, claimSet);
        assertThrows(IssuerServerException.class, () -> AuditService.maskJWT(jwtWithOutSignature));
    }

    @NotNull
    private static JWT createAccessToken(final String fnr) throws ParseException {
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder().subject(fnr).build();
        JWSHeader header = new JWSHeader(new JWSAlgorithm(JWSAlgorithm.RS256.getName()));
        return new SignedJWT(header.toBase64URL(), claimSet.toPayload().toBase64URL(), new Base64URL("signature"));
    }

}