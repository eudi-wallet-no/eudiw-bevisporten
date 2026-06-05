package no.idporten.eudiw.issuer.logging.audit;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.context.CredentialRevokeContext;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
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
import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static no.idporten.eudiw.issuer.TestData.*;
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

        auditService.logCreateCredentialOffer("issuer", List.of("credentialConfigId"));

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertEquals(AuditID.CREATE_CREDENTIAL_OFFER.auditIdentifier().auditId(), actualEntry.getAuditId().auditId());
        assertEquals("issuer", actualEntry.getAttributes().get(AuditService.CREDENTIAL_ISSUER));
        assertEquals(Collections.singletonList("credentialConfigId"), actualEntry.getAttributes().get(AuditService.CREDENTIAL_CONFIGURATION_ID));
    }

    @DisplayName("then all audit entries are logged correctly for IssueCredentials")
    @Test
    void testLogIssueCredentials() throws ParseException {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId("33");
        NotificationId notificationId = new NotificationId("222");
        JWT jwt = createAccessToken("12345678901");
        CredentialIssueContext context = new CredentialIssueContext(jwt, junitIssuerTenant(), credentialConfigurationFromClasspath("credential-configurations/junit_mso_mdoc.json"));

        auditService.logIssueCredentials(context, issuanceTransactionId, 2, notificationId);

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertAll(
                () -> assertEquals(AuditID.ISSUE_CREDENTIAL.auditIdentifier().auditId(), actualEntry.getAuditId().auditId()),
                () -> assertEquals("12345678901", actualEntry.getAttributes().get(AuditService.SUBJECT)),
                () -> assertEquals("junit", actualEntry.getAttributes().get(AuditService.AUTHORIZATION_SERVER)),
                () -> assertEquals("junitdoc_mso_mdoc", actualEntry.getAttributes().get(AuditService.CREDENTIAL_CONFIGURATION_ID)),
                () -> assertEquals("mso_mdoc", actualEntry.getAttributes().get(AuditService.FORMAT)),
                () -> assertEquals(2, actualEntry.getAttributes().get(AuditService.INSTANCES)),
                () -> assertEquals(notificationId.getValue(), actualEntry.getAttributes().get(AuditService.NOTIFICATION_ID)),
                () -> assertEquals(issuanceTransactionId.getValue(), actualEntry.getAttributes().get(AuditService.ISSUANCE_TRANSACTION_ID)),
                () -> assertNotNull(actualEntry.getAttributes().get(AuditService.ACCESS_TOKEN))
        );
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
        assertEquals(notificationId.getValue(), actualEntry.getAttributes().get(AuditService.NOTIFICATION_ID));
        assertEquals(status, actualEntry.getAttributes().get(AuditService.STATUS));
    }

    @DisplayName("then all audit entries are logged correctly for RevokeCredential")
    @Test
    void testLogRevokeCredential() throws ParseException {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId("99");
        String status = "INVALID";
        CredentialRevokeContext context = new CredentialRevokeContext(
                createAccessToken("12345678901"),
                junitIssuerTenant(),
                credentialConfigurationFromClasspath("credential-configurations/junit_mso_mdoc.json"),
                issuanceTransactionId
        );
        List<StatusEntry> entries = List.of(
                new StatusEntry(1, URI.create("https://status.eidas2sandkasse.no/lists/1")),
                new StatusEntry(5, URI.create("https://status.eidas2sandkasse.no/lists/1")),
                new StatusEntry(2, URI.create("https://status.eidas2sandkasse.no/lists/1")),
                new StatusEntry(20, URI.create("https://status.eidas2sandkasse.no/lists/1")),
                new StatusEntry(1, URI.create("https://status.eidas2sandkasse.no/lists/2")),
                new StatusEntry(3, URI.create("https://status.eidas2sandkasse.no/lists/2"))
        );

        auditService.logRevokeCredential(context, entries, status);

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertEquals(AuditID.REVOKE_CREDENTIAL.auditIdentifier().auditId(), actualEntry.getAuditId().auditId());
        assertEquals("junitdoc_mso_mdoc", actualEntry.getAttributes().get(AuditService.CREDENTIAL_CONFIGURATION_ID));
        assertEquals(issuanceTransactionId.getValue(), actualEntry.getAttributes().get(AuditService.ISSUANCE_TRANSACTION_ID));
        assertNotNull(actualEntry.getAttributes().get(AuditService.ACCESS_TOKEN));

        JSONObject compact = (JSONObject) actualEntry.getAttributes().get(AuditService.TOKEN_STATUS_LIST);
        assertEquals(6, compact.get("count"));
        assertEquals(status, compact.get("status"));
        JSONArray compactEntries = (JSONArray) compact.get("entries");
        assertEquals(2, compactEntries.size());

        JSONObject first = (JSONObject) compactEntries.get(0);
        assertEquals("https://status.eidas2sandkasse.no/lists/1", first.get("uri"));
        assertEquals(4, first.get("count"));
        assertEquals(List.of(1, 5, 2, 20), first.get("indexes"));

        JSONObject second = (JSONObject) compactEntries.get(1);
        assertEquals("https://status.eidas2sandkasse.no/lists/2", second.get("uri"));
        assertEquals(2, second.get("count"));
        assertEquals(List.of(1, 3), second.get("indexes"));
    }

    @DisplayName("then all audit entries are logged correctly for IssueCredentialStatus")
    @Test
    void testLogIssueCredentialStatus() throws ParseException {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        CredentialIssueContext context = new CredentialIssueContext(
                createAccessToken("12345678901"),
                junitIssuerTenant(),
                credentialConfigurationFromClasspath("credential-configurations/junit_mso_mdoc.json")
        );
        List<StatusEntry> entries = List.of(
                new StatusEntry(1, URI.create("https://status.eidas2sandkasse.no/lists/1")),
                new StatusEntry(5, URI.create("https://status.eidas2sandkasse.no/lists/1")),
                new StatusEntry(2, URI.create("https://status.eidas2sandkasse.no/lists/1")),
                new StatusEntry(20, URI.create("https://status.eidas2sandkasse.no/lists/1")),
                new StatusEntry(1, URI.create("https://status.eidas2sandkasse.no/lists/2")),
                new StatusEntry(3, URI.create("https://status.eidas2sandkasse.no/lists/2"))
        );

        auditService.logIssueCredentialStatus(context, entries);

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertEquals(AuditID.ISSUE_CREDENTIAL_STATUS.auditIdentifier().auditId(), actualEntry.getAuditId().auditId());
        assertEquals("junitdoc_mso_mdoc", actualEntry.getAttributes().get(AuditService.CREDENTIAL_CONFIGURATION_ID));

        JSONObject compact = (JSONObject) actualEntry.getAttributes().get(AuditService.TOKEN_STATUS_LIST);
        assertEquals(6, compact.get("count"));
        assertFalse(compact.containsKey("status"));
        JSONArray compactEntries = (JSONArray) compact.get("entries");
        assertEquals(2, compactEntries.size());

        JSONObject first = (JSONObject) compactEntries.get(0);
        assertEquals("https://status.eidas2sandkasse.no/lists/1", first.get("uri"));
        assertEquals(4, first.get("count"));
        assertEquals(List.of(1, 5, 2, 20), first.get("indexes"));

        JSONObject second = (JSONObject) compactEntries.get(1);
        assertEquals("https://status.eidas2sandkasse.no/lists/2", second.get("uri"));
        assertEquals(2, second.get("count"));
        assertEquals(List.of(1, 3), second.get("indexes"));
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