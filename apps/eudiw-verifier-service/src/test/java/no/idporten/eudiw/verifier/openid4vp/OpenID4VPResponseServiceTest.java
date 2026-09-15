package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import id.walt.mdoc.doc.MDoc;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.VerificationResult;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.api.openid4vp.EncryptedAuthorizationResponse;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.statuslist.TokenStatuslistService;
import no.idporten.eudiw.verifier.trustlist.TrustlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When processing OpenID4VP responses")
class OpenID4VPResponseServiceTest {

    private static final String TRANSACTION_ID = "transaction-id";
    private static final String STATE = "state";

    @Mock VerificationTransactionService verificationService;
    @Mock TokenStatuslistService tokenStatuslistService;
    @Mock TrustlistService trustlistService;
    @Mock MDocService mDocService;
    @Mock SdJwtService sdJwtService;
    @Mock MDoc mDoc;
    @Mock SDJwt sdJwt;
    @Mock VerificationResult<SDJwt> sdJwtResult;
    @Mock X509Certificate certificate;

    private OpenID4VPResponseService service;
    private ClientApplication client;
    private ECKey encryptionKey;

    @BeforeEach
    void setUp() throws Exception {
        service = new OpenID4VPResponseService(
                verificationService, tokenStatuslistService, trustlistService, mDocService, sdJwtService);
        client = new ClientApplication();
        client.setId("client");
        encryptionKey = new ECKeyGenerator(Curve.P_256).generate();
    }

    @Test
    void rejectsUnknownTransactionBeforeDecrypting() {
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(null);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, response("not-a-jwe")));

        assertEquals("Unknown verification transaction id", exception.getErrorDescription());
        verifyNoInteractions(tokenStatuslistService, trustlistService, mDocService, sdJwtService);
    }

    @Test
    void decryptsRealJweAndPersistsMissingSelectedCredential() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        Map<String, Object> payload = Map.of("state", STATE, "nonce", "nonce", "vp_token", Map.of());

        var callback = service.receiveResponse(client, TRANSACTION_ID, encrypted(payload));

        ArgumentCaptor<VerifiedCredentials> credentials = ArgumentCaptor.forClass(VerifiedCredentials.class);
        ArgumentCaptor<Map<String, Object>> persistedPayload = ArgumentCaptor.forClass(Map.class);
        verify(verificationService).addVerifiedCredentials(
                eq(client), eq(TRANSACTION_ID), credentials.capture(), persistedPayload.capture());
        VerifiedCredential missing = credentials.getValue().credentials().get("pid").getFirst();
        assertAll(
                () -> assertFalse(missing.valid()),
                () -> assertEquals(Map.of(), missing.claims()),
                () -> assertEquals(List.of(), missing.validationDetails()),
                () -> assertEquals(STATE, persistedPayload.getValue().get("state")),
                () -> assertNull(callback.getRedirectUri()));
        verifyNoInteractions(mDocService, sdJwtService);
    }

    @Test
    void rejectsInvalidState() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID,
                        encrypted(Map.of("state", "wrong", "vp_token", Map.of()))));

        assertEquals("Invalid state in authorization response", exception.getErrorDescription());
        verify(verificationService, never()).addVerifiedCredentials(any(), anyString(), any(), any());
    }

    @Test
    void rejectsMissingAndEmptyCredentialQueries() throws Exception {
        for (DcqlQuery query : java.util.Arrays.asList(null, new DcqlQuery(), dcql(List.of()))) {
            VerificationTransaction transaction = transaction(query);
            when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
            VerificationException exception = assertThrows(VerificationException.class,
                    () -> service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(Map.of()))));
            assertEquals("Missing credentials in dcql_query", exception.getErrorDescription());
        }
    }

    @Test
    void rejectsBlankCredentialId() throws Exception {
        VerificationTransaction transaction = transaction(query(" ", "mso_mdoc"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(Map.of()))));

        assertEquals("Missing id in dcql_query credential", exception.getErrorDescription());
    }

    @Test
    void rejectsUnsupportedFormatWhenCredentialWasSelected() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "jwt_vc_json"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID,
                        encrypted(validPayload(Map.of("pid", "token")))));

        assertEquals("Unsupported credential format: jwt_vc_json", exception.getErrorDescription());
    }

    @Test
    void rejectsNonMapVpToken() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload("token"))));

        assertEquals("Unsupported vp_token structure", exception.getErrorDescription());
        verify(verificationService, never()).addVerifiedCredentials(any(), anyString(), any(), any());
    }

    @Test
    void rejectsInvalidVpTokenValueAndStructure() throws Exception {
        assertVpTokenFailure(Map.of("pid", List.of("token", 1)),
                "Unsupported vp_token value type for credential id: pid");
        assertVpTokenFailure(Map.of("pid", Map.of("nested", "token")),
                "Unsupported vp_token structure for credential id: pid");
    }

    @Test
    void routesMdocAndCombinesValidationStatusesAndDetails() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc"));
        transaction.setIncludeValidationDetails(true);
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        when(mDocService.mDocFromVpToken("mdoc-token")).thenReturn(mDoc);
        when(mDocService.claimsFromMDoc(mDoc)).thenReturn(Map.of("family_name", "Nordmann"));
        when(mDocService.verifyMDoc(mDoc)).thenReturn(ValidationStatus.VALID);
        when(mDocService.extractCertificateFromMdoc(mDoc)).thenReturn(certificate);
        when(mDocService.extractStatuslistUriAndIdx(mDoc)).thenReturn(null);
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate))
                .thenReturn(ValidationStatus.VALID);
        when(tokenStatuslistService.getValidationDetail(ValidationStatus.NOT_APPLICABLE)).thenReturn("no status");
        when(trustlistService.getValidationDetail(ValidationStatus.VALID)).thenReturn("trusted");
        when(mDocService.getValidationDetail(ValidationStatus.VALID)).thenReturn("valid mdoc");

        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("pid", "mdoc-token"))));

        VerifiedCredential credential = persistedCredential("pid");
        assertAll(
                () -> assertTrue(credential.valid()),
                () -> assertEquals("Nordmann", credential.claims().get("family_name")),
                () -> assertEquals(3, credential.validationDetails().size()),
                () -> assertEquals("no status", credential.validationDetails().get(0).validationDetails()),
                () -> assertEquals("trusted", credential.validationDetails().get(1).validationDetails()),
                () -> assertEquals("valid mdoc", credential.validationDetails().get(2).validationDetails()));
        verify(mDocService).mDocFromVpToken("mdoc-token");
        verifyNoInteractions(sdJwtService);
    }

    @Test
    void routesSdJwtAndMarksCombinedInvalidOutcome() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "dc+sd-jwt"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        when(sdJwtService.sdJwtFromVpToken("sd-token")).thenReturn(sdJwt);
        when(sdJwtService.certificate(sdJwt)).thenReturn(certificate);
        when(sdJwtService.verifySdJwt(sdJwt, certificate)).thenReturn(sdJwtResult);
        when(sdJwtService.validationStatusSdJwt(sdJwtResult)).thenReturn(ValidationStatus.VALID);
        when(sdJwtService.sdJwtClaims(sdJwtResult)).thenReturn(Map.of("given_name", "Ola"));
        when(sdJwtService.extractStatuslistUriAndIdx(sdJwtResult))
                .thenReturn(new StatuslistEntry("7", URI.create("https://status.example/list")));
        when(sdJwtResult.getSdJwt()).thenReturn(sdJwt);
        when(tokenStatuslistService.lookupStatusFromStatuslist(URI.create("https://status.example/list"), 7))
                .thenReturn(ValidationStatus.INVALID);
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate))
                .thenReturn(ValidationStatus.VALID);

        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("pid", List.of("sd-token")))));

        VerifiedCredential credential = persistedCredential("pid");
        assertAll(
                () -> assertFalse(credential.valid()),
                () -> assertEquals("Ola", credential.claims().get("given_name")),
                () -> assertNull(credential.validationDetails()));
        verify(sdJwtService).verifySdJwt(sdJwt, certificate);
        verify(tokenStatuslistService)
                .lookupStatusFromStatuslist(URI.create("https://status.example/list"), 7);
        verifyNoInteractions(mDocService);
    }

    @Test
    void statusListValidProducesValidCredential() throws Exception {
        prepareMdocWithStatus("0", ValidationStatus.VALID);
        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("pid", "mdoc-token"))));
        assertTrue(persistedCredential("pid").valid());
    }

    @Test
    void rejectsMalformedAndOverflowingStatusListIndexes() throws Exception {
        for (String idx : List.of("not-a-number", "2147483648", "-1", "-2147483649")) {
            reset(verificationService, mDocService, trustlistService, tokenStatuslistService);
            prepareMdocWithStatus(idx, ValidationStatus.VALID);
            VerificationException exception = assertThrows(VerificationException.class,
                    () -> service.receiveResponse(client, TRANSACTION_ID,
                            encrypted(validPayload(Map.of("pid", "mdoc-token")))));
            assertEquals("Invalid status list idx in vp_token", exception.getErrorDescription());
            verify(tokenStatuslistService, never()).lookupStatusFromStatuslist(any(), anyInt());
        }
    }

    @Test
    void acceptsIntegerStatusListBoundaries() throws Exception {
        for (int idx : List.of(0, Integer.MAX_VALUE)) {
            reset(verificationService, mDocService, trustlistService, tokenStatuslistService);
            prepareMdocWithStatus(Integer.toString(idx), ValidationStatus.VALID);
            service.receiveResponse(client, TRANSACTION_ID,
                    encrypted(validPayload(Map.of("pid", "mdoc-token"))));
            verify(tokenStatuslistService)
                    .lookupStatusFromStatuslist(URI.create("https://status.example/list"), idx);
        }
    }

    @Test
    void returnsRedirectOnlyForSameDeviceFlow() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc"));
        transaction.setFlow("same_device");
        transaction.setRedirectUri(URI.create("https://client.example/callback"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        var callback = service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(Map.of())));

        assertEquals(URI.create("https://client.example/callback"), callback.getRedirectUri());
    }

    private void prepareMdocWithStatus(String idx, ValidationStatus status) {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        when(mDocService.mDocFromVpToken("mdoc-token")).thenReturn(mDoc);
        when(mDocService.claimsFromMDoc(mDoc)).thenReturn(Map.of());
        when(mDocService.verifyMDoc(mDoc)).thenReturn(ValidationStatus.VALID);
        when(mDocService.extractCertificateFromMdoc(mDoc)).thenReturn(certificate);
        when(mDocService.extractStatuslistUriAndIdx(mDoc))
                .thenReturn(new StatuslistEntry(idx, URI.create("https://status.example/list")));
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate))
                .thenReturn(ValidationStatus.VALID);
        if (idx.matches("-?\\d{1,10}")) {
            try {
                when(tokenStatuslistService.lookupStatusFromStatuslist(
                        URI.create("https://status.example/list"), Integer.parseInt(idx))).thenReturn(status);
            } catch (NumberFormatException ignored) {
                // The service under test rejects values outside the integer range.
            }
        }
    }

    private void assertVpTokenFailure(Map<?, ?> vpToken, String message) throws Exception {
        reset(verificationService);
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(vpToken))));
        assertEquals(message, exception.getErrorDescription());
    }

    private VerifiedCredential persistedCredential(String id) {
        ArgumentCaptor<VerifiedCredentials> captor = ArgumentCaptor.forClass(VerifiedCredentials.class);
        verify(verificationService).addVerifiedCredentials(eq(client), eq(TRANSACTION_ID), captor.capture(), any());
        return captor.getValue().credentials().get(id).getFirst();
    }

    private VerificationTransaction transaction(DcqlQuery query) {
        VerificationTransaction transaction = new VerificationTransaction();
        transaction.setState(STATE);
        transaction.setEncryptionKey(encryptionKey);
        transaction.setDcqlQuery(query);
        return transaction;
    }

    private static DcqlQuery query(String id, String format) {
        DcqlCredentialQuery credential = new DcqlCredentialQuery();
        credential.setId(id);
        credential.setFormat(format);
        return dcql(List.of(credential));
    }

    private static DcqlQuery dcql(List<DcqlCredentialQuery> credentials) {
        DcqlQuery query = new DcqlQuery();
        query.setCredentials(credentials);
        return query;
    }

    private static Map<String, Object> validPayload(Object vpToken) {
        return Map.of("state", STATE, "nonce", "nonce", "vp_token", vpToken);
    }

    private EncryptedAuthorizationResponse encrypted(Map<String, Object> payload) throws Exception {
        JWEObject jwe = new JWEObject(
                new JWEHeader(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM),
                new Payload(payload));
        jwe.encrypt(new ECDHEncrypter(encryptionKey.toPublicJWK()));
        return response(jwe.serialize());
    }

    private static EncryptedAuthorizationResponse response(String value) {
        EncryptedAuthorizationResponse response = new EncryptedAuthorizationResponse();
        response.setResponse(value);
        return response;
    }
}
