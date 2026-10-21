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
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.VerificationResult;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonObject;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.api.openid4vp.EncryptedAuthorizationResponse;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.statuslist.TokenStatuslistService;
import no.idporten.eudiw.verifier.trustlist.TrustlistFormat;
import no.idporten.eudiw.verifier.trustlist.TrustlistReference;
import no.idporten.eudiw.verifier.trustlist.TrustlistService;
import no.idporten.eudiw.verifier.trustlist.TrustlistsProperties;
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
    private static final String PID_VCT = "urn:eudi:pid:1";
    private static final TrustlistReference PID_TRUSTLIST = new TrustlistReference(
            URI.create("https://tillitsliste.test.eidas2sandkasse.net/no_eidas2sandkasse_test_pid.jws"), TrustlistFormat.ETSI_602_JSON);
    private static final TrustlistReference WEBUILD_TRUSTLIST = new TrustlistReference(
            URI.create("https://trustlist.webuild.jwt"), TrustlistFormat.ETSI_602_JSON);
    private static final TrustlistReference ATTESTATION_TRUSTLIST = new TrustlistReference(
            URI.create("https://tillitsliste.test.eidas2sandkasse.net/no_eidas2sandkasse_test_tsl.xtsl"), TrustlistFormat.ETSI_612_XML);
    private static final List<TrustlistReference> PID_TRUSTLISTS = List.of(PID_TRUSTLIST, WEBUILD_TRUSTLIST);
    private static final List<TrustlistReference> ATTESTATION_TRUSTLISTS = List.of(ATTESTATION_TRUSTLIST);
    private static final String PID_DOC_TYPE = "eu.europa.ec.eudi.pid.1";

    @Mock VerificationTransactionService verificationService;
    @Mock TokenStatuslistService tokenStatuslistService;
    @Mock TrustlistService trustlistService;
    @Mock TrustlistsProperties trustlistsProperties;
    @Mock MDocService mDocService;
    @Mock SdJwtService sdJwtService;
   // @Mock MDoc mDoc;
    @Mock SDJwt sdJwt;
    @Mock VerificationResult<SDJwt> sdJwtResult;
    @Mock X509Certificate certificate;

    private OpenID4VPResponseService service;
    private ClientApplication client;
    private ECKey encryptionKey;

    @BeforeEach
    void setUp() throws Exception {
        service = new OpenID4VPResponseService(
                verificationService, tokenStatuslistService, trustlistService, trustlistsProperties, mDocService, sdJwtService);
        lenient().when(trustlistsProperties.getPidTrustlists()).thenReturn(PID_TRUSTLISTS);
        lenient().when(trustlistsProperties.getAttestationTrustlists()).thenReturn(ATTESTATION_TRUSTLISTS);
        client = new ClientApplication();
        client.setId("client");
        encryptionKey = new ECKeyGenerator(Curve.P_256).generate();
    }

    @Test
    @DisplayName("without a known transaction, then rejection before decryption is expected")
    void rejectsUnknownTransactionBeforeDecrypting() {
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(null);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, response("not-a-jwe")));

        assertEquals("Unknown verification transaction id", exception.getErrorDescription());
        verifyNoInteractions(tokenStatuslistService, trustlistService, mDocService, sdJwtService);
    }

    @Test
    @DisplayName("without an authorization response, then error status is expected")
    void rejectsMissingAuthorizationResponse() {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, response(" ")));

        assertEquals("Missing authorization response", exception.getErrorDescription());
        verify(verificationService).markAsError(client, TRANSACTION_ID);
    }

    @Test
    @DisplayName("with an unencrypted authorization response, then error status is expected")
    void rejectsUnencryptedAuthorizationResponse() {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, response("not-a-jwe")));

        assertEquals("Authorization response is not encrypted", exception.getErrorDescription());
        verify(verificationService).markAsError(client, TRANSACTION_ID);
    }

    @Test
    @DisplayName("with a real JWE and a missing selected credential, then missing credential persistence is expected")
    void decryptsRealJweAndPersistsMissingSelectedCredential() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
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
    @DisplayName("with an invalid state, then invalid request is expected")
    void rejectsInvalidState() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID,
                        encrypted(Map.of("state", "wrong", "vp_token", Map.of()))));

        assertEquals("Invalid state in authorization response", exception.getErrorDescription());
        verify(verificationService).markAsError(client, TRANSACTION_ID);
        verify(verificationService, never()).addVerifiedCredentials(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("without credential queries or with an empty query, then invalid request is expected")
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
    @DisplayName("with a blank credential ID, then invalid request is expected")
    void rejectsBlankCredentialId() throws Exception {
        VerificationTransaction transaction = transaction(query(" ", "mso_mdoc", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(Map.of()))));

        assertEquals("Missing id in dcql_query credential", exception.getErrorDescription());
    }

    @Test
    @DisplayName("with a selected credential in an unsupported format, then invalid request is expected")
    void rejectsUnsupportedFormatWhenCredentialWasSelected() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "jwt_vc_json", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID,
                        encrypted(validPayload(Map.of("pid", "token")))));

        assertEquals("Unsupported credential format: jwt_vc_json", exception.getErrorDescription());
    }

    @Test
    @DisplayName("with a non-map vp_token, then invalid request is expected")
    void rejectsNonMapVpToken() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload("token"))));

        assertEquals("Unsupported vp_token structure", exception.getErrorDescription());
        verify(verificationService).markAsError(client, TRANSACTION_ID);
        verify(verificationService, never()).addVerifiedCredentials(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("when persisting error status fails, then the response failure is preserved")
    void preservesResponseFailureWhenErrorStatusPersistenceFails() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        IllegalStateException persistenceException = new IllegalStateException("Cache unavailable");
        doThrow(persistenceException)
                .when(verificationService)
                .markAsError(client, TRANSACTION_ID);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.receiveResponse(
                        client,
                        TRANSACTION_ID,
                        encrypted(validPayload("invalid-vp-token"))));

        assertAll(
                () -> assertEquals("Unsupported vp_token structure", exception.getErrorDescription()),
                () -> assertArrayEquals(
                        new Throwable[]{persistenceException},
                        exception.getSuppressed()));
    }

    @Test
    @DisplayName("with invalid vp_token values or structures, then invalid request is expected")
    void rejectsInvalidVpTokenValueAndStructure() throws Exception {
        assertVpTokenFailure(Map.of("pid", List.of("token", 1)),
                "Unsupported vp_token value type for credential id: pid");
        assertVpTokenFailure(Map.of("pid", Map.of("nested", "token")),
                "Unsupported vp_token structure for credential id: pid");
    }

    @Test
    @DisplayName("with an SD-JWT credential and invalid status, then an invalid combined outcome is expected")
    void routesSdJwtAndMarksCombinedInvalidOutcome() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "dc+sd-jwt", true));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        when(sdJwtService.sdJwtFromVpToken("sd-token")).thenReturn(sdJwt);
        when(sdJwt.getFullPayload()).thenReturn(payloadWithVct(PID_VCT));
        when(sdJwtService.certificate(sdJwt)).thenReturn(certificate);
        when(sdJwtService.verifySdJwt(sdJwt, certificate)).thenReturn(sdJwtResult);
        when(sdJwtService.validationStatusSdJwt(sdJwtResult)).thenReturn(ValidationStatus.VALID);
        when(sdJwtService.sdJwtClaims(sdJwtResult)).thenReturn(Map.of("given_name", "Ola"));
        when(sdJwtService.extractStatuslistUriAndIdx(sdJwtResult))
                .thenReturn(new StatuslistEntry("7", URI.create("https://status.example/list")));
        when(sdJwtResult.getSdJwt()).thenReturn(sdJwt);
        when(tokenStatuslistService.lookupStatusFromStatuslist(URI.create("https://status.example/list"), 7))
                .thenReturn(ValidationStatus.INVALID);
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, PID_TRUSTLISTS))
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
    @DisplayName("with a non-PID SD-JWT credential, then the attestation trustlists are used")
    void routesNonPidSdJwtToAttestationTrustlists() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "dc+sd-jwt", true));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        when(sdJwtService.sdJwtFromVpToken("sd-token")).thenReturn(sdJwt);
        when(sdJwt.getFullPayload()).thenReturn(payloadWithVct("urn:example:attestation:1"));
        when(sdJwtService.certificate(sdJwt)).thenReturn(certificate);
        when(sdJwtService.verifySdJwt(sdJwt, certificate)).thenReturn(sdJwtResult);
        when(sdJwtService.validationStatusHolderBinding(transaction, sdJwt, true)).thenReturn(ValidationStatus.VALID);
        when(sdJwtService.validationStatusSdJwt(sdJwtResult)).thenReturn(ValidationStatus.VALID);
        when(sdJwtService.sdJwtClaims(sdJwtResult)).thenReturn(Map.of());
        when(sdJwtService.extractStatuslistUriAndIdx(sdJwtResult)).thenReturn(null);
        when(sdJwtResult.getSdJwt()).thenReturn(sdJwt);
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, ATTESTATION_TRUSTLISTS))
                .thenReturn(ValidationStatus.VALID);

        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("pid", "sd-token"))));

        assertTrue(persistedCredential("pid").valid());
        verify(trustlistService).checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, ATTESTATION_TRUSTLISTS);
    }


    @Test
    @DisplayName("with a same-device flow, then a redirect is expected")
    void returnsRedirectOnlyForSameDeviceFlow() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        transaction.setFlow("same_device");
        transaction.setRedirectUri(URI.create("https://client.example/callback"));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        var callback = service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(Map.of())));

        assertEquals(URI.create("https://client.example/callback"), callback.getRedirectUri());
    }

    @Test
    @DisplayName("with requireCryptographicHolderBinding not present, then cryptographic holder binding is required by default")
    void requiresCryptographicHolderBindingByDefault() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "dc+sd-jwt", null));
        assertTrue(transaction.getDcqlQuery().getCredentials().getFirst().getRequireCryptographicHolderBinding());
    }


    private void assertVpTokenFailure(Map<?, ?> vpToken, String message) throws Exception {
        reset(verificationService);
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
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

    private static DcqlQuery query(String id, String format, Boolean requireCryptographicHolderBinding) {
        DcqlCredentialQuery credential = new DcqlCredentialQuery();
        credential.setId(id);
        credential.setFormat(format);
        credential.setRequireCryptographicHolderBinding(requireCryptographicHolderBinding);
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

    private static JsonObject payloadWithVct(String vct) {
        return (JsonObject) Json.Default.parseToJsonElement("{\"vct\":\"%s\"}".formatted(vct));
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
