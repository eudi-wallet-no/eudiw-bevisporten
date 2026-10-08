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
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.jspecify.annotations.NonNull;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
                () -> assertTrue(authorizationRequest.getQuery().contains(expectedRequestUri())));
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
        configureExistingTransaction();
        String serializedRequest = service.retrieveAuthorizationRequest(clientApplication, REQUEST_ID, SAME_DEVICE_FLOW);

        SignedJWT request = SignedJWT.parse(serializedRequest);
        assertTrue(request.verify(getEcdsaVerifier(keyProvider)));
        VerificationTransaction storedTransaction = captureStoredTransaction();

        assertAll(
                () -> assertEquals(EXTERNAL_BASE_URI, request.getJWTClaimsSet().getIssuer()),
                () -> assertEquals(SAME_DEVICE_FLOW, storedTransaction.getFlow()),
                () -> assertEquals(request.getJWTClaimsSet().toJSONObject(), storedTransaction.getRequest()),
                () -> assertNotNull(storedTransaction.getState()),
                () -> assertNotNull(storedTransaction.getEncryptionKey()),
                () -> assertNull(request.getJWTClaimsSet().getClaim("dcql_query")));
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
        var claims = query.getCredentials().get(0).getClaims();
        var sdJwtClaims = query.getCredentials().get(1).getClaims();
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
