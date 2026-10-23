package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.api.verification.StartVerificationRequest;
import no.idporten.eudiw.verifier.cache.CacheService;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.config.VerifierServiceProperties;
import no.idporten.eudiw.verifier.logging.audit.AuditID;
import no.idporten.eudiw.verifier.logging.audit.AuditService;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlClaimQuery;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlTrustedAuthoritiesQuery;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When handling OpenID4VP authorization requests")
class OpenID4VPRequestServiceTest {

    private static final String CLIENT_ID = "junit";
    private static final String TRANSACTION_ID = "transaction-id";
    private static final String EXTERNAL_BASE_URI = "https://verifier.eidas2sandkasse.dev";
    private static final String SIOP2_CLIENT_ID = "verifier.eidas2sandkasse.dev";
    private static final String REQUEST_ID = "request-id";
    private static final String SAME_DEVICE_FLOW = "same_device";
    private static final String CROSS_DEVICE_FLOW = "cross_device";
    private static final String KEYSTORE_NAME = "junit-keystore";
    private static final String PNG_DATA_URI_PREFIX = "data:image/png;base64,";

    @Mock
    private KeystoreManager keystoreManager;
    @Mock
    private VerificationTransactionService verificationTransactionService;
    @Mock
    private CacheService cacheService;
    @Mock
    private KeyProvider keyProvider;
    @Mock
    private X509Certificate certificate;
    @Mock
    private AuditService auditService;

    private OpenID4VPRequestService service;
    private ClientApplication clientApplication;

    @BeforeEach
    void setUp() throws Exception {
        VerifierServiceProperties properties = new VerifierServiceProperties();
        properties.setExternalBaseUri(EXTERNAL_BASE_URI);
        properties.setSiop2ClientId(SIOP2_CLIENT_ID);

        clientApplication = new ClientApplication();
        clientApplication.setId(CLIENT_ID);
        clientApplication.setKeystoreName(KEYSTORE_NAME);

        service = new OpenID4VPRequestService(
                properties,
                keystoreManager,
                verificationTransactionService,
                JsonMapper.builder().build(),
                cacheService,
                auditService);
        lenient().when(keystoreManager.getKeyProvider(KEYSTORE_NAME)).thenReturn(keyProvider);
        lenient().when(keyProvider.certificate()).thenReturn(certificate);
        lenient().when(certificate.getEncoded()).thenReturn(new byte[]{1, 2, 3});
    }

    @ParameterizedTest
    @ValueSource(strings = {SAME_DEVICE_FLOW, CROSS_DEVICE_FLOW})
    @DisplayName("When creating an authorization request without a configured scheme, then the default scheme is expected")
    void createsAuthorizationRequestUri(String flow) {
        URI authorizationRequest = service.createAuthorizationRequest(
                REQUEST_ID, clientApplication, flow);

        assertAll(
                () -> assertEquals("eudi-openid4vp", authorizationRequest.getScheme()),
                () -> assertEquals(SIOP2_CLIENT_ID, authorizationRequest.getHost()),
                () -> assertTrue(authorizationRequest.getQuery().contains("client_id=x509_hash:")),
                () -> assertTrue(authorizationRequest.getQuery().contains(expectedRequestUri(flow))));
    }

    @ParameterizedTest
    @ValueSource(strings = {SAME_DEVICE_FLOW, CROSS_DEVICE_FLOW})
    @DisplayName("When creating an authorization request with a client scheme, then only the URI scheme is expected to change")
    void createsAuthorizationRequestWithClientScheme(String flow) {
        URI defaultRequest = service.createAuthorizationRequest(REQUEST_ID, clientApplication, flow);
        clientApplication.setAuthorizationRequestScheme("eu-eaap");

        URI authorizationRequest = service.createAuthorizationRequest(REQUEST_ID, clientApplication, flow);

        assertAll(
                () -> assertEquals("eu-eaap", authorizationRequest.getScheme()),
                () -> assertEquals(defaultRequest.getRawSchemeSpecificPart(), authorizationRequest.getRawSchemeSpecificPart()));
    }

    @Test
    @DisplayName("When creating a client id, then the SHA-256 hash of the certificate is returned")
    void createsClientIdFromCertificateHash() {
        String clientId = service.makeClientId(clientApplication);

        assertEquals("x509_hash:A5BYxvLAy0ksUzsKTRTvd8wPeKvMztUofYShogEc-4E", clientId);
    }

    @Test
    @DisplayName("When creating a request id, then the authorization request is cached")
    void createsAndCachesRequestId() {
        String requestId = service.createRequestId(clientApplication, TRANSACTION_ID);

        assertDoesNotThrow(() -> UUID.fromString(requestId));
        verify(cacheService).putAuthorizationRequest(clientApplication, requestId, TRANSACTION_ID);
    }

    @Test
    @DisplayName("When creating a QR code data URI, then a PNG data URI is returned")
    void createsQrCodeDataUri() throws Exception {
        URI qrCode = service.createQrCodeDataURI(qrCodeContent());

        byte[] png = decodePng(qrCode);

        assertTrue(qrCode.toString().startsWith(PNG_DATA_URI_PREFIX));
        assertArrayEquals(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47}, Arrays.copyOf(png, 4));
    }

    @Test
    @DisplayName("When retrieving an unknown authorization request, then invalid request is expected")
    void rejectsUnknownAuthorizationRequest() {
        String unknownRequestId = "unknown";
        when(cacheService.retrieveAuthorizationRequest(clientApplication, unknownRequestId)).thenReturn(null);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.retrieveAuthorizationRequest(clientApplication, unknownRequestId, SAME_DEVICE_FLOW));

        assertAll(
                () -> assertEquals("invalid_request", exception.getError()),
                () -> assertEquals("Unknown authorization request", exception.getErrorDescription()));
        verifyNoInteractions(verificationTransactionService);
    }

    @Test
    @DisplayName("When the authorization request has an unknown transaction, then invalid request is expected")
    void rejectsUnknownVerificationTransaction() {
        when(cacheService.retrieveAuthorizationRequest(clientApplication, REQUEST_ID))
                .thenReturn(TRANSACTION_ID);
        when(verificationTransactionService.getVerificationTransaction(clientApplication, TRANSACTION_ID))
                .thenReturn(null);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.retrieveAuthorizationRequest(
                        clientApplication, REQUEST_ID, SAME_DEVICE_FLOW));

        assertEquals("Unknown verification transaction", exception.getErrorDescription());
    }

    @Test
    @DisplayName("When a verification transaction exists, then a signed authorization request is stored and returned")
    void createsAndStoresSignedAuthorizationRequest() throws Exception {
        VerificationTransaction transaction = configureExistingTransaction();
        String serializedRequest = service.retrieveAuthorizationRequest(clientApplication, REQUEST_ID, SAME_DEVICE_FLOW);
        verify(auditService, times(1)).auditFetchAuthzRequest(anyString(), eq(clientApplication), eq(REQUEST_ID), eq(SAME_DEVICE_FLOW));
        verify(auditService, times(1)).auditFetchAuthzResponse(eq(transaction), anyString(), eq(clientApplication), eq(REQUEST_ID));

        SignedJWT request = SignedJWT.parse(serializedRequest);
        assertTrue(request.verify(getEcdsaVerifier(keyProvider)));
        VerificationTransaction storedTransaction = captureStoredTransaction();

        assertAll(
                () -> assertEquals(EXTERNAL_BASE_URI, request.getJWTClaimsSet().getIssuer()),
                () -> assertEquals(SAME_DEVICE_FLOW, storedTransaction.getFlow()),
                () -> assertEquals(request.getJWTClaimsSet().toJSONObject(), storedTransaction.getRequest()),
                () -> assertNotNull(storedTransaction.getState()),
                () -> assertNotNull(storedTransaction.getEncryptionKey()),
                () -> assertFalse(request.getJWTClaimsSet().toJSONObject().containsKey("verifier_info")),
                () -> assertNull(request.getJWTClaimsSet().getClaim("dcql_query")));
    }

    @Test
    @DisplayName("When signing an authorization request, then the protected header contains the same iat as the payload")
    void includesIssuedAtInProtectedHeader() throws Exception {
        configureExistingTransaction();

        SignedJWT request = SignedJWT.parse(service.retrieveAuthorizationRequest(
                clientApplication, REQUEST_ID, SAME_DEVICE_FLOW));
        Object headerIssuedAt = request.getHeader().getCustomParam("iat");

        assertAll(
                () -> assertTrue(request.verify(getEcdsaVerifier(keyProvider))),
                () -> assertInstanceOf(Long.class, headerIssuedAt),
                () -> assertEquals(request.getJWTClaimsSet().getIssueTime().getTime() / 1000, headerIssuedAt),
                () -> assertEquals(120_000, request.getJWTClaimsSet().getExpirationTime().getTime()
                        - request.getJWTClaimsSet().getIssueTime().getTime()));
    }

    @Test
    @DisplayName("When only one verifier info attestation is configured, then only that JSON object is included")
    void includesOneConfiguredVerifierInfoAttestation() throws Exception {
        clientApplication.setRegistrarDataset("""
                {"format":"registrar_dataset","data":{"name":"Test Registrar"}}
                """);
        clientApplication.setWalletRelyingPartyRegistrationCertificate(" ");
        configureExistingTransaction();

        SignedJWT request = SignedJWT.parse(service.retrieveAuthorizationRequest(
                clientApplication, REQUEST_ID, SAME_DEVICE_FLOW));
        List<?> verifierInfo = (List<?>) request.getJWTClaimsSet().getClaim("verifier_info");

        assertAll(
                () -> assertTrue(request.verify(getEcdsaVerifier(keyProvider))),
                () -> assertEquals(1, verifierInfo.size()),
                () -> assertEquals(Map.of(
                        "format", "registrar_dataset",
                        "data", Map.of("name", "Test Registrar")
                ), verifierInfo.getFirst())
        );
    }

    @Test
    @DisplayName("When both verifier info attestations are configured, then both JSON objects are included in order")
    void includesBothConfiguredVerifierInfoAttestations() throws Exception {
        clientApplication.setRegistrarDataset("""
                {"format":"registrar_dataset","data":{"name":"Test Registrar"}}
                """);
        clientApplication.setWalletRelyingPartyRegistrationCertificate("""
                {"format":"jwt","data":"eyJhbGciOiJFUzI1NiJ9.payload.signature"}
                """);
        configureExistingTransaction();

        SignedJWT request = SignedJWT.parse(service.retrieveAuthorizationRequest(
                clientApplication, REQUEST_ID, SAME_DEVICE_FLOW));
        List<?> verifierInfo = (List<?>) request.getJWTClaimsSet().getClaim("verifier_info");

        assertAll(
                () -> assertTrue(request.verify(getEcdsaVerifier(keyProvider))),
                () -> assertEquals(2, verifierInfo.size()),
                () -> assertEquals(Map.of(
                        "format", "registrar_dataset",
                        "data", Map.of("name", "Test Registrar")
                ), verifierInfo.get(0)),
                () -> assertEquals(Map.of(
                        "format", "jwt",
                        "data", "eyJhbGciOiJFUzI1NiJ9.payload.signature"
                ), verifierInfo.get(1))
        );
    }

    @Test
    @DisplayName("When a configured verifier info attestation is not valid JSON, then a server error is expected")
    void rejectsMalformedVerifierInfoAttestation() throws Exception {
        clientApplication.setRegistrarDataset("not-json");
        VerificationTransaction transaction = new VerificationTransaction();
        transaction.setClientApplication(clientApplication);
        transaction.setEncryptionKey(new ECKeyGenerator(Curve.P_256).generate());
        when(cacheService.retrieveAuthorizationRequest(clientApplication, REQUEST_ID))
                .thenReturn(TRANSACTION_ID);
        when(verificationTransactionService.getVerificationTransaction(clientApplication, TRANSACTION_ID))
                .thenReturn(transaction);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.retrieveAuthorizationRequest(clientApplication, REQUEST_ID, SAME_DEVICE_FLOW));

        assertAll(
                () -> assertEquals("server_error", exception.getError()),
                () -> assertEquals("Failed to parse configured verifier info attestation", exception.getErrorDescription())
        );
    }

    private @NonNull ECDSAVerifier getEcdsaVerifier(KeyProvider keyProvider1) throws JOSEException {
        return new ECDSAVerifier((java.security.interfaces.ECPublicKey) keyProvider1.publicKey());
    }

    @Test
    @DisplayName("When signing a mixed query with retention choices, then mdoc values are preserved and SD-JWT values are omitted")
    void includesOnlyMdocRetentionInSignedRequest() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        StartVerificationRequest input = mapper.readValue("""
                {
                  "dcql_query": {
                    "credentials": [
                      {
                        "id": "pid",
                        "format": "mso_mdoc",
                        "meta": {"doctype_value": "eu.europa.ec.eudi.pid.1"},
                        "claims": [
                          {"id": "family_name", "path": ["eu.europa.ec.eudi.pid.1", "family_name"]},
                          {"id": "given_name", "path": ["eu.europa.ec.eudi.pid.1", "given_name"], "intent_to_retain": true},
                          {"id": "birth_date", "path": ["eu.europa.ec.eudi.pid.1", "birth_date"], "intent_to_retain": false}
                        ]
                      },
                      {
                        "id": "contact",
                        "format": "dc+sd-jwt",
                        "meta": {"vct_values": ["urn:example:contact"]},
                        "claims": [
                          {"path": ["email"]},
                          {"path": ["given_name"], "intent_to_retain": true},
                          {"path": ["family_name"], "intent_to_retain": false}
                        ]
                      }
                    ]
                  }
                }
                """, StartVerificationRequest.class);
        new VerificationTransactionService(cacheService).initTransaction(
                input.dcqlQuery(), null, TRANSACTION_ID, clientApplication, false);
        ArgumentCaptor<VerificationTransaction> captor = ArgumentCaptor.forClass(VerificationTransaction.class);
        verify(cacheService).putVerificationTransaction(eq(clientApplication), eq(TRANSACTION_ID), captor.capture());
        configureExistingTransaction();
        when(verificationTransactionService.getVerificationTransaction(clientApplication, TRANSACTION_ID))
                .thenReturn(captor.getValue());

        SignedJWT request = SignedJWT.parse(service.retrieveAuthorizationRequest(
                clientApplication, REQUEST_ID, SAME_DEVICE_FLOW));

        String serializedQuery = mapper.writeValueAsString(request.getJWTClaimsSet().getClaim("dcql_query"));
        DcqlQuery query = mapper.readValue(serializedQuery, DcqlQuery.class);
        List<DcqlClaimQuery> claims = query.getCredentials().get(0).getClaims();
        List<DcqlClaimQuery> sdJwtClaims = query.getCredentials().get(1).getClaims();
        assertAll(
                () -> assertTrue(request.verify(getEcdsaVerifier(keyProvider))),
                () -> assertTrue(claims.get(0).getIntentToRetain()),
                () -> assertTrue(claims.get(1).getIntentToRetain()),
                () -> assertFalse(claims.get(2).getIntentToRetain()),
                () -> assertEquals("family_name", claims.get(0).getId()),
                () -> assertEquals(List.of("eu.europa.ec.eudi.pid.1", "family_name"), claims.get(0).getPath()),
                () -> assertEquals(3, sdJwtClaims.size()),
                () -> assertEquals(List.of("email"), sdJwtClaims.get(0).getPath()),
                () -> assertEquals(List.of("given_name"), sdJwtClaims.get(1).getPath()),
                () -> assertEquals(List.of("family_name"), sdJwtClaims.get(2).getPath()),
                () -> assertNull(sdJwtClaims.get(0).getIntentToRetain()),
                () -> assertNull(sdJwtClaims.get(1).getIntentToRetain()),
                () -> assertNull(sdJwtClaims.get(2).getIntentToRetain()),
                () -> assertFalse(serializedQuery.contains("\"intent_to_retain\":null")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mso_mdoc", "dc+sd-jwt"})
    @DisplayName("When a query contains trusted authorities, then unchanged values in the cached transaction and signed request are expected")
    void forwardsTrustedAuthoritiesThroughCacheAndSignedRequest(String format) throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        String inputJson = """
                {
                  "dcql_query": {
                    "credentials": [
                      {
                        "id": "with_authorities",
                        "format": "%1$s",
                        "meta": {},
                        "trusted_authorities": [
                          {"type": "aki", "values": ["s9tIpPmhxdiuNkHMEWNpYim8S8Y", "AQID"]},
                          {"type": "etsi_tl", "values": ["https://lotl.eidas2sandkasse.dev", "https://other.eidas2sandkasse.dev"]},
                          {"type": "openid_federation", "values": ["https://trustanchor.eidas2sandkasse.dev"]},
                          {"type": "custom_framework", "values": ["urn:example:trust"]}
                        ]
                      },
                      {
                        "id": "different_authorities",
                        "format": "%1$s",
                        "meta": {},
                        "trusted_authorities": [
                          {"type": "etsi_tl", "values": ["https://another.eidas2sandkasse.dev"]}
                        ]
                      },
                      {"id": "omitted_authorities", "format": "%1$s", "meta": {}},
                      {"id": "null_authorities", "format": "%1$s", "meta": {}, "trusted_authorities": null},
                      {"id": "empty_authorities", "format": "%1$s", "meta": {}, "trusted_authorities": []}
                    ]
                  }
                }
                """.formatted(format);
        StartVerificationRequest input = mapper.readValue(inputJson, StartVerificationRequest.class);
        VerificationTransaction transaction = configureExistingTransaction();
        transaction.setDcqlQuery(input.dcqlQuery());
        assertEquals(input.dcqlQuery(), transaction.getDcqlQuery());
        SignedJWT request = SignedJWT.parse(service.retrieveAuthorizationRequest(clientApplication, REQUEST_ID, SAME_DEVICE_FLOW));

        String serializedQuery = mapper.writeValueAsString(request.getJWTClaimsSet().getClaim("dcql_query"));
        DcqlQuery query = mapper.readValue(serializedQuery, DcqlQuery.class);
        List<DcqlCredentialQuery> expectedCredentials = input.dcqlQuery().getCredentials();
        List<DcqlCredentialQuery> actualCredentials = query.getCredentials();
        List<DcqlTrustedAuthoritiesQuery> authorities = actualCredentials.getFirst().getTrustedAuthorities();
        assertAll(
                () -> assertTrue(request.verify(getEcdsaVerifier(keyProvider))),
                () -> assertEquals(expectedCredentials.size(), actualCredentials.size()),
                () -> assertEquals(List.of("aki", "etsi_tl", "openid_federation", "custom_framework"),
                        authorities.stream().map(DcqlTrustedAuthoritiesQuery::getType).toList()),
                () -> assertEquals(List.of(
                                List.of("s9tIpPmhxdiuNkHMEWNpYim8S8Y", "AQID"),
                                List.of("https://lotl.eidas2sandkasse.dev", "https://other.eidas2sandkasse.dev"),
                                List.of("https://trustanchor.eidas2sandkasse.dev"),
                                List.of("urn:example:trust")),
                        authorities.stream().map(DcqlTrustedAuthoritiesQuery::getValues).toList()));
        for (int i = 0; i < expectedCredentials.size(); i++) {
            DcqlCredentialQuery expectedCredential = expectedCredentials.get(i);
            DcqlCredentialQuery actualCredential = actualCredentials.get(i);
            assertEquals(expectedCredential.getId(), actualCredential.getId());
            assertEquals(format, actualCredential.getFormat());
            if (CollectionUtils.isEmpty(expectedCredential.getTrustedAuthorities())) {
                assertNull(actualCredential.getTrustedAuthorities());
            } else {
                assertEquals(expectedCredential.getTrustedAuthorities(), actualCredential.getTrustedAuthorities());
            }
        }
    }

    private String expectedRequestUri(String flow) {
        return "request_uri=%s/openid4vp/authz-request/%s/%s?flow=%s"
                .formatted(EXTERNAL_BASE_URI, CLIENT_ID, REQUEST_ID, flow);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mso_mdoc", "dc+sd-jwt"})
    @DisplayName("When multiple is omitted, null, false or true, then effective booleans in the transaction and signed request are expected")
    void forwardsMultipleThroughTransactionAndSignedRequest(String format) throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        StartVerificationRequest input = mapper.readValue("""
                {
                  "dcql_query": {
                    "credentials": [
                      {"id": "omitted", "format": "%1$s", "meta": {}},
                      {"id": "null_value", "format": "%1$s", "meta": {}, "multiple": null},
                      {"id": "single", "format": "%1$s", "meta": {}, "multiple": false},
                      {"id": "many", "format": "%1$s", "meta": {}, "multiple": true}
                    ]
                  }
                }
                """.formatted(format), StartVerificationRequest.class);
        VerificationTransactionService transactionService = new VerificationTransactionService(cacheService);
        transactionService.initTransaction(input.dcqlQuery(), null, TRANSACTION_ID, clientApplication, false);
        ArgumentCaptor<VerificationTransaction> initialTransaction = ArgumentCaptor.forClass(VerificationTransaction.class);
        verify(cacheService).putVerificationTransaction(eq(clientApplication), eq(TRANSACTION_ID), initialTransaction.capture());
        DcqlQuery storedQuery = mapper.readValue(
                mapper.writeValueAsString(initialTransaction.getValue().getDcqlQuery()), DcqlQuery.class);
        VerificationTransaction transaction = configureExistingTransaction();
        transaction.setDcqlQuery(storedQuery);

        SignedJWT request = SignedJWT.parse(service.retrieveAuthorizationRequest(clientApplication, REQUEST_ID, SAME_DEVICE_FLOW));

        DcqlQuery signedQuery = mapper.convertValue(request.getJWTClaimsSet().getClaim("dcql_query"), DcqlQuery.class);
        assertAll(
                () -> assertTrue(request.verify(getEcdsaVerifier(keyProvider))),
                () -> assertEquals(List.of(false, false, false, true),
                        storedQuery.getCredentials().stream().map(DcqlCredentialQuery::isMultiple).toList()),
                () -> assertEquals(List.of(false, false, false, true),
                        signedQuery.getCredentials().stream().map(DcqlCredentialQuery::isMultiple).toList()),
                () -> assertEquals(mapper.convertValue(signedQuery, Map.class), request.getJWTClaimsSet().getClaim("dcql_query")),
                () -> assertEquals(storedQuery, captureStoredTransaction().getDcqlQuery()));
    }

    private URI qrCodeContent() {
        return URI.create("eudi-openid4vp://%s?request_uri=test".formatted(SIOP2_CLIENT_ID));
    }

    private byte[] decodePng(URI qrCode) {
        String encodedPng = qrCode.toString().substring(PNG_DATA_URI_PREFIX.length());
        return java.util.Base64.getDecoder().decode(encodedPng);
    }

    private VerificationTransaction configureExistingTransaction() throws Exception {
        ECKey signingKey = new ECKeyGenerator(Curve.P_256).generate();
        VerificationTransaction transaction = new VerificationTransaction();
        transaction.setClientApplication(clientApplication);
        transaction.setEncryptionKey(new ECKeyGenerator(Curve.P_256).generate());
        when(cacheService.retrieveAuthorizationRequest(clientApplication, REQUEST_ID))
                .thenReturn(TRANSACTION_ID);
        when(verificationTransactionService.getVerificationTransaction(clientApplication, TRANSACTION_ID))
                .thenReturn(transaction);
        when(keyProvider.privateKey()).thenReturn(signingKey.toECPrivateKey());
        when(keyProvider.publicKey()).thenReturn(signingKey.toECPublicKey());
        return transaction;
    }

    private VerificationTransaction captureStoredTransaction() {
        ArgumentCaptor<VerificationTransaction> transactionCaptor =
                ArgumentCaptor.forClass(VerificationTransaction.class);
        verify(cacheService).updateVerificationTransaction(
                eq(clientApplication), eq(TRANSACTION_ID), transactionCaptor.capture());
        return transactionCaptor.getValue();
    }
}
