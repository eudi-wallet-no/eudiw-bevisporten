package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.cache.CacheService;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.config.VerifierServiceProperties;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("When handling OpenID4VP authorization requests")
class OpenID4VPRequestServiceTest {

    private static final String CLIENT_ID = "junit";
    private static final String TRANSACTION_ID = "transaction-id";
    private static final String EXTERNAL_BASE_URI = "https://verifier.example";
    private static final String SIOP2_CLIENT_ID = "verifier.example";
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
                cacheService);
        lenient().when(keystoreManager.getKeyProvider(KEYSTORE_NAME)).thenReturn(keyProvider);
        lenient().when(keyProvider.certificate()).thenReturn(certificate);
        lenient().when(certificate.getEncoded()).thenReturn(new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("When creating an authorization request, then the expected URI is returned")
    void createsAuthorizationRequestUri() {
        URI authorizationRequest = service.createAuthorizationRequest(
                REQUEST_ID, clientApplication, CROSS_DEVICE_FLOW);

        assertAll(
                () -> assertEquals("eudi-openid4vp", authorizationRequest.getScheme()),
                () -> assertEquals(SIOP2_CLIENT_ID, authorizationRequest.getHost()),
                () -> assertTrue(authorizationRequest.getQuery().contains("client_id=x509_hash:")),
                () -> assertTrue(authorizationRequest.getQuery().contains(
                        expectedRequestUri())));
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
        configureExistingTransaction();

        String serializedRequest = service.retrieveAuthorizationRequest(
                clientApplication, REQUEST_ID, SAME_DEVICE_FLOW);

        SignedJWT request = SignedJWT.parse(serializedRequest);
        VerificationTransaction storedTransaction = captureStoredTransaction();

        assertAll(
                () -> assertEquals("https://self-issued.me/v2", request.getJWTClaimsSet().getAudience().getFirst()),
                () -> assertEquals(EXTERNAL_BASE_URI, request.getJWTClaimsSet().getIssuer()),
                () -> assertEquals(SAME_DEVICE_FLOW, storedTransaction.getFlow()),
                () -> assertEquals(request.getJWTClaimsSet().toJSONObject(), storedTransaction.getRequest()),
                () -> assertNotNull(storedTransaction.getState()),
                () -> assertNotNull(storedTransaction.getEncryptionKey()),
                () -> assertNull(request.getJWTClaimsSet().getClaim("dcql_query")));
    }

    private String expectedRequestUri() {
        return "request_uri=%s/openid4vp/authz-request/%s/%s?flow=%s"
                .formatted(EXTERNAL_BASE_URI, CLIENT_ID, REQUEST_ID, CROSS_DEVICE_FLOW);
    }

    private URI qrCodeContent() {
        return URI.create("eudi-openid4vp://%s?request_uri=test".formatted(SIOP2_CLIENT_ID));
    }

    private byte[] decodePng(URI qrCode) {
        String encodedPng = qrCode.toString().substring(PNG_DATA_URI_PREFIX.length());
        return java.util.Base64.getDecoder().decode(encodedPng);
    }

    private void configureExistingTransaction() throws Exception {
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
    }

    private VerificationTransaction captureStoredTransaction() {
        ArgumentCaptor<VerificationTransaction> transactionCaptor =
                ArgumentCaptor.forClass(VerificationTransaction.class);
        verify(cacheService).updateVerificationTransaction(
                eq(clientApplication), eq(TRANSACTION_ID), transactionCaptor.capture());
        return transactionCaptor.getValue();
    }
}
