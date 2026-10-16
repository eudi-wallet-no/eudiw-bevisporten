package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.mdoc.dataelement.StringElement;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
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
import no.idporten.eudiw.verifier.testdata.Certificates;
import no.idporten.eudiw.verifier.trustlist.TrustlistFormat;
import no.idporten.eudiw.verifier.trustlist.TrustlistReference;
import no.idporten.eudiw.verifier.trustlist.TrustlistService;
import no.idporten.eudiw.verifier.trustlist.TrustlistsProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.verifier.openid4vp.MDocServiceTest.VALID_VP_TOKEN;
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
                        encrypted(validPayload(Map.of("pid", List.of("token"))))));

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
    @DisplayName("when credential processing fails, then error status is expected")
    void marksCredentialProcessingFailureAsError() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        DeviceResponse deviceResponse = mDocService.deviceResponseFromVpToken("mdoc-token");
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        VerificationException processingException =
                new VerificationException("invalid_request", "Invalid mdoc");
        when(mDocService.mDocFromDeviceResponse(deviceResponse)).thenThrow(processingException);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.receiveResponse(
                        client,
                        TRANSACTION_ID,
                        encrypted(validPayload(Map.of("pid", List.of("mdoc-token"))))));

        assertSame(processingException, exception);
        verify(verificationService).markAsError(client, TRANSACTION_ID);
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
        assertVpTokenFailure(Map.of("pid", List.of()),
                "Empty presentation array for credential id: pid");
    }

    @ParameterizedTest
    @CsvSource({
            "mso_mdoc,", "mso_mdoc,false", "mso_mdoc,true",
            "dc+sd-jwt,", "dc+sd-jwt,false", "dc+sd-jwt,true"
    })
    @DisplayName("then a single String verified credential is expected for backwards compatibility")
    void acceptsPresentationAsString(String format, Boolean multiple) throws Exception {
        DcqlQuery dcqlQuery = query("pid", format, true);
        if (multiple != null) {
            dcqlQuery.getCredentials().getFirst().setMultiple(multiple);
        }
        VerificationTransaction transaction = transaction(dcqlQuery);
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        preparePresentation(format, "token", transaction, ValidationStatus.VALID);

        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("pid", "token"))));

        ArgumentCaptor<VerifiedCredentials> credentials = ArgumentCaptor.forClass(VerifiedCredentials.class);
        verify(verificationService).addVerifiedCredentials(eq(client), eq(TRANSACTION_ID), credentials.capture(), any());
        List<VerifiedCredential> results = credentials.getValue().credentials().get("pid");
        assertAll(
                () -> assertEquals(List.of(Map.of("value", "token")),
                        results.stream().map(VerifiedCredential::claims).toList()),
                () -> assertEquals(List.of(true), results.stream().map(VerifiedCredential::valid).toList()));
        verify(verificationService, never()).markAsError(any(), anyString());
    }

    @ParameterizedTest
    @CsvSource({"mso_mdoc,", "mso_mdoc,false", "dc+sd-jwt,", "dc+sd-jwt,false"})
    @DisplayName("When multiple is omitted or false with two presentations, then rejection and error status are expected")
    void rejectsMultiplePresentationsWithoutPermission(String format, Boolean multiple) throws Exception {
        DcqlQuery dcqlQuery = query("pid", format, true);
        if (multiple != null) {
            dcqlQuery.getCredentials().getFirst().setMultiple(multiple);
        }
        VerificationTransaction transaction = transaction(dcqlQuery);
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID,
                        encrypted(validPayload(Map.of("pid", List.of("first", "second"))))));

        assertAll(
                () -> assertEquals("invalid_request", exception.getError()),
                () -> assertEquals("Multiple presentations are not allowed for credential id: pid",
                        exception.getErrorDescription()));
        verify(verificationService).markAsError(client, TRANSACTION_ID);
        verify(verificationService, never()).addVerifiedCredentials(any(), anyString(), any(), any());
        verifyNoInteractions(mDocService, sdJwtService, tokenStatuslistService, trustlistService);
    }

    @ParameterizedTest
    @CsvSource({
            "mso_mdoc,,1", "mso_mdoc,false,1", "mso_mdoc,true,1", "mso_mdoc,true,2",
            "dc+sd-jwt,,1", "dc+sd-jwt,false,1", "dc+sd-jwt,true,1", "dc+sd-jwt,true,2"
    })
    @DisplayName("When the presentation count is permitted, then all independently verified results are expected")
    void verifiesPermittedPresentations(String format, Boolean multiple, int count) throws Exception {
        DcqlQuery dcqlQuery = query("pid", format, true);
        if (multiple != null) {
            dcqlQuery.getCredentials().getFirst().setMultiple(multiple);
        }
        VerificationTransaction transaction = transaction(dcqlQuery);
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        List<String> tokens = List.of("first", "second").subList(0, count);
        for (String token : tokens) {
            preparePresentation(format, token, transaction, ValidationStatus.VALID);
        }

        service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(Map.of("pid", tokens))));

        ArgumentCaptor<VerifiedCredentials> credentials = ArgumentCaptor.forClass(VerifiedCredentials.class);
        verify(verificationService).addVerifiedCredentials(eq(client), eq(TRANSACTION_ID), credentials.capture(), any());
        List<VerifiedCredential> results = credentials.getValue().credentials().get("pid");
        assertAll(
                () -> assertEquals(count, results.size()),
                () -> assertEquals(tokens.stream().map(token -> Map.of("value", token)).toList(),
                        results.stream().map(VerifiedCredential::claims).toList()),
                () -> assertTrue(results.stream().allMatch(VerifiedCredential::valid)));
        verify(verificationService, never()).markAsError(any(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"mso_mdoc", "dc+sd-jwt"})
    @DisplayName("When queries have different multiple settings, then independent counts and validation results are expected")
    void handlesMultiplePerQueryAndPreservesInvalidResults(String format) throws Exception {
        DcqlCredentialQuery multipleQuery = query("many", format, true).getCredentials().getFirst();
        multipleQuery.setMultiple(true);
        DcqlCredentialQuery singleQuery = query("single", format, true).getCredentials().getFirst();
        VerificationTransaction transaction = transaction(dcql(List.of(multipleQuery, singleQuery)));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        preparePresentation(format, "first", transaction, ValidationStatus.VALID);
        preparePresentation(format, "second", transaction, ValidationStatus.INVALID);
        preparePresentation(format, "third", transaction, ValidationStatus.VALID);

        service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(
                Map.of("many", List.of("first", "second"), "single", List.of("third")))));

        ArgumentCaptor<VerifiedCredentials> credentials = ArgumentCaptor.forClass(VerifiedCredentials.class);
        verify(verificationService).addVerifiedCredentials(eq(client), eq(TRANSACTION_ID), credentials.capture(), any());
        Map<String, List<VerifiedCredential>> results = credentials.getValue().credentials();
        assertAll(
                () -> assertEquals(List.of(true, false), results.get("many").stream().map(VerifiedCredential::valid).toList()),
                () -> assertEquals(List.of(Map.of("value", "first"), Map.of("value", "second")),
                        results.get("many").stream().map(VerifiedCredential::claims).toList()),
                () -> assertEquals(List.of(true), results.get("single").stream().map(VerifiedCredential::valid).toList()),
                () -> assertEquals(List.of(Map.of("value", "third")),
                        results.get("single").stream().map(VerifiedCredential::claims).toList()));
    }

    @Test
    @DisplayName("When another query permits multiple presentations, then a single-only query still rejects multiple presentations")
    void rejectsMultipleForSingleQueryWhenAnotherAllowsIt() throws Exception {
        DcqlCredentialQuery multipleQuery = query("many", "mso_mdoc", true).getCredentials().getFirst();
        multipleQuery.setMultiple(true);
        DcqlCredentialQuery singleQuery = query("single", "mso_mdoc", true).getCredentials().getFirst();
        VerificationTransaction transaction = transaction(dcql(List.of(singleQuery, multipleQuery)));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(
                        Map.of("many", List.of("first", "second"), "single", List.of("third", "fourth"))))));

        assertEquals("Multiple presentations are not allowed for credential id: single", exception.getErrorDescription());
        verify(verificationService).markAsError(client, TRANSACTION_ID);
        verify(verificationService, never()).addVerifiedCredentials(any(), anyString(), any(), any());
        verifyNoInteractions(mDocService, sdJwtService, tokenStatuslistService, trustlistService);
    }

    @Test
    @DisplayName("When multiple is true with an empty array, then rejection rather than missing-credential persistence is expected")
    void rejectsEmptyArrayWithMultipleAllowed() throws Exception {
        DcqlQuery dcqlQuery = query("pid", "mso_mdoc", true);
        dcqlQuery.getCredentials().getFirst().setMultiple(true);
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction(dcqlQuery));

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID,
                        encrypted(validPayload(Map.of("pid", List.of())))));

        assertEquals("Empty presentation array for credential id: pid", exception.getErrorDescription());
        verify(verificationService).markAsError(client, TRANSACTION_ID);
        verify(verificationService, never()).addVerifiedCredentials(any(), anyString(), any(), any());
        verifyNoInteractions(mDocService, sdJwtService, tokenStatuslistService, trustlistService);
    }

    @Test
    @DisplayName("with an mdoc credential, then routing and combined validation details are expected")
    void routesMdocAndCombinesValidationStatusesAndDetails() throws Exception {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        DeviceResponse deviceResponse = mDocService.deviceResponseFromVpToken("mdoc-token");
        transaction.setIncludeValidationDetails(true);
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        when(mDocService.mDocFromDeviceResponse(deviceResponse)).thenReturn(mDoc);
        when(mDoc.getDocType()).thenReturn(new StringElement(PID_DOC_TYPE));
        when(mDocService.claimsFromMDoc(mDoc)).thenReturn(Map.of("family_name", "Nordmann"));
        when(mDocService.verifyMDoc(mDoc, transaction)).thenReturn(ValidationStatus.VALID);
        when(mDocService.extractCertificateFromMdoc(mDoc)).thenReturn(certificate);
        when(mDocService.extractStatuslistUriAndIdx(mDoc)).thenReturn(null);
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, PID_TRUSTLISTS))
                .thenReturn(ValidationStatus.VALID);
        when(tokenStatuslistService.getValidationDetail(ValidationStatus.NOT_APPLICABLE)).thenReturn("no status");
        when(trustlistService.getValidationDetail(ValidationStatus.VALID)).thenReturn("trusted");
        when(mDocService.getValidationDetail(ValidationStatus.VALID)).thenReturn("valid mdoc");

        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("pid", List.of("mdoc-token")))));

        VerifiedCredential credential = persistedCredential("pid");
        assertAll(
                () -> assertTrue(credential.valid()),
                () -> assertEquals("Nordmann", credential.claims().get("family_name")),
                () -> assertEquals(3, credential.validationDetails().size()),
                () -> assertEquals("no status", credential.validationDetails().get(0).validationDetails()),
                () -> assertEquals("trusted", credential.validationDetails().get(1).validationDetails()),
                () -> assertEquals("valid mdoc", credential.validationDetails().get(2).validationDetails()));
        verify(mDocService).mDocFromDeviceResponse(deviceResponse);
        verifyNoInteractions(sdJwtService);
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
        when(sdJwtService.validateSdJwtTimeClaims(sdJwtResult)).thenReturn(ValidationStatus.VALID);
        when(sdJwtService.validationStatusSdJwt(sdJwtResult, ValidationStatus.VALID)).thenReturn(ValidationStatus.VALID);
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
    @DisplayName("with a valid status list entry, then a valid credential is expected")
    void statusListValidProducesValidCredential() throws Exception {
        prepareMdocWithStatus("0", ValidationStatus.VALID);
        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("pid", List.of("mdoc-token")))));
        assertTrue(persistedCredential("pid").valid());
    }

    @Test
    @DisplayName("with a non-PID mdoc credential, then the default trustlist is used")
    void routesNonPidMdocToDefaultTrustlist() throws Exception {
        VerificationTransaction transaction = transaction(query("attestation", "mso_mdoc", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        DeviceResponse deviceResponse = mDocService.deviceResponseFromVpToken("mdoc-token");
        when(mDocService.mDocFromDeviceResponse(deviceResponse)).thenReturn(mDoc);
        when(mDoc.getDocType()).thenReturn(new StringElement("eu.example.attestation.1"));
        when(mDocService.claimsFromMDoc(mDoc)).thenReturn(Map.of());
        when(mDocService.verifyMDoc(mDoc, transaction)).thenReturn(ValidationStatus.VALID);
        when(mDocService.extractCertificateFromMdoc(mDoc)).thenReturn(certificate);
        when(mDocService.extractStatuslistUriAndIdx(mDoc)).thenReturn(null);
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, ATTESTATION_TRUSTLISTS))
                .thenReturn(ValidationStatus.VALID);

        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("attestation", List.of("mdoc-token")))));

        assertTrue(persistedCredential("attestation").valid());
        verify(trustlistService).checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, ATTESTATION_TRUSTLISTS);
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
        when(sdJwtService.validateSdJwtTimeClaims(sdJwtResult)).thenReturn(ValidationStatus.VALID);
        when(sdJwtService.validationStatusSdJwt(sdJwtResult, ValidationStatus.VALID)).thenReturn(ValidationStatus.VALID);
        when(sdJwtService.sdJwtClaims(sdJwtResult)).thenReturn(Map.of());
        when(sdJwtService.extractStatuslistUriAndIdx(sdJwtResult)).thenReturn(null);
        when(sdJwtResult.getSdJwt()).thenReturn(sdJwt);
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, ATTESTATION_TRUSTLISTS))
                .thenReturn(ValidationStatus.VALID);

        service.receiveResponse(client, TRANSACTION_ID,
                encrypted(validPayload(Map.of("pid", List.of("sd-token")))));

        assertTrue(persistedCredential("pid").valid());
        verify(trustlistService).checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, ATTESTATION_TRUSTLISTS);
    }

    @Test
    @DisplayName("with malformed or overflowing status list indexes, then invalid request is expected")
    void rejectsMalformedAndOverflowingStatusListIndexes() throws Exception {
        for (String idx : List.of("not-a-number", "2147483648", "-1", "-2147483649")) {
            reset(verificationService, mDocService, trustlistService, tokenStatuslistService);
            prepareMdocWithStatus(idx, ValidationStatus.VALID);
            VerificationException exception = assertThrows(VerificationException.class,
                    () -> service.receiveResponse(client, TRANSACTION_ID,
                            encrypted(validPayload(Map.of("pid", List.of("mdoc-token"))))));
            assertEquals("Invalid status list idx in vp_token", exception.getErrorDescription());
            verify(tokenStatuslistService, never()).lookupStatusFromStatuslist(any(), anyInt());
        }
    }

    @Test
    @DisplayName("with integer status list boundaries, then successful lookups are expected")
    void acceptsIntegerStatusListBoundaries() throws Exception {
        for (int idx : List.of(0, Integer.MAX_VALUE)) {
            reset(verificationService, mDocService, trustlistService, tokenStatuslistService);
            prepareMdocWithStatus(Integer.toString(idx), ValidationStatus.VALID);
            service.receiveResponse(client, TRANSACTION_ID,
                    encrypted(validPayload(Map.of("pid", List.of("mdoc-token")))));
            verify(tokenStatuslistService)
                    .lookupStatusFromStatuslist(URI.create("https://status.example/list"), idx);
        }
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

    @Test
    @DisplayName("with a bad issuer signature, then invalid request is expected")
    void validationStatusNegativeWhenWrongIssuerSignature() {
        Security.addProvider(new BouncyCastleProvider());
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));

        MDocService realService = new MDocService();
        MDocService service = spy(realService);

        DeviceResponse deviceResponse = realService.deviceResponseFromVpToken(VALID_VP_TOKEN);
        MDoc realMdoc = realService.mDocFromDeviceResponse(deviceResponse);
        Certificates certs = new Certificates();
        X509Certificate negativeTestCert = X509CertUtils.parse("-----BEGIN CERTIFICATE-----"+ certs.webuildPidDigdirCertificate() +"-----END CERTIFICATE-----");

        doReturn(negativeTestCert)
                .when(service)
                .extractCertificateFromMdoc(realMdoc);

        ValidationStatus status = service.verifyMDoc(realMdoc, transaction);

        assertEquals(ValidationStatus.INVALID, status);
    }

    private void preparePresentation(String format, String token, VerificationTransaction transaction, ValidationStatus status) {
        X509Certificate presentationCertificate = mock(X509Certificate.class);
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(presentationCertificate, PID_TRUSTLISTS))
                .thenReturn(ValidationStatus.VALID);
        if ("mso_mdoc".equals(format)) {
            DeviceResponse deviceResponse = mock(DeviceResponse.class);
            MDoc document = mock(MDoc.class);
            when(mDocService.deviceResponseFromVpToken(token)).thenReturn(deviceResponse);
            when(mDocService.mDocFromDeviceResponse(deviceResponse)).thenReturn(document);
            when(document.getDocType()).thenReturn(new StringElement(PID_DOC_TYPE));
            when(mDocService.claimsFromMDoc(document)).thenReturn(Map.of("value", token));
            when(mDocService.verifyMDoc(document, transaction)).thenReturn(status);
            when(mDocService.extractCertificateFromMdoc(document)).thenReturn(presentationCertificate);
        } else {
            SDJwt presentation = mock(SDJwt.class);
            VerificationResult<SDJwt> result = mock();
            when(result.getSdJwt()).thenReturn(presentation);
            when(sdJwtService.sdJwtFromVpToken(token)).thenReturn(presentation);
            when(presentation.getFullPayload()).thenReturn(payloadWithVct(PID_VCT));
            when(sdJwtService.certificate(presentation)).thenReturn(presentationCertificate);
            when(sdJwtService.verifySdJwt(presentation, presentationCertificate)).thenReturn(result);
            when(sdJwtService.validationStatusHolderBinding(transaction, presentation, true)).thenReturn(ValidationStatus.VALID);
            when(sdJwtService.validateSdJwtTimeClaims(result)).thenReturn(ValidationStatus.VALID);
            when(sdJwtService.validationStatusSdJwt(result, ValidationStatus.VALID)).thenReturn(status);
            when(sdJwtService.sdJwtClaims(result)).thenReturn(Map.of("value", token));
        }
    }

    private void prepareMdocWithStatus(String idx, ValidationStatus status) {
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        DeviceResponse deviceResponse = mDocService.deviceResponseFromVpToken("mdoc-token");
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        when(mDocService.mDocFromDeviceResponse(deviceResponse)).thenReturn(mDoc);
        when(mDoc.getDocType()).thenReturn(new StringElement(PID_DOC_TYPE));
        when(mDocService.claimsFromMDoc(mDoc)).thenReturn(Map.of());
        when(mDocService.verifyMDoc(mDoc, transaction)).thenReturn(ValidationStatus.VALID);
        when(mDocService.extractCertificateFromMdoc(mDoc)).thenReturn(certificate);
        when(mDocService.extractStatuslistUriAndIdx(mDoc))
                .thenReturn(new StatuslistEntry(idx, URI.create("https://status.example/list")));
        when(trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(certificate, PID_TRUSTLISTS))
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
        VerificationTransaction transaction = transaction(query("pid", "mso_mdoc", false));
        when(verificationService.getVerificationTransaction(client, TRANSACTION_ID)).thenReturn(transaction);
        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.receiveResponse(client, TRANSACTION_ID, encrypted(validPayload(vpToken))));
        assertEquals(message, exception.getErrorDescription());
        verify(verificationService).markAsError(client, TRANSACTION_ID);
        verify(verificationService, never()).addVerifiedCredentials(any(), anyString(), any(), any());
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