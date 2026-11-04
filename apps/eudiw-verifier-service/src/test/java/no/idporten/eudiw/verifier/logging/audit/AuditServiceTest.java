package no.idporten.eudiw.verifier.logging.audit;

import no.idporten.eudiw.verifier.api.verification.StartVerificationRequest;
import no.idporten.eudiw.verifier.api.verification.StartVerificationResponse;
import no.idporten.eudiw.verifier.api.verification.VerificationResultResponse;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.VerifiablePresentation;
import no.idporten.eudiw.verifier.openid4vp.VerificationTransaction;
import no.idporten.eudiw.verifier.openid4vp.VpToken;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("When audit logging is used")
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogger, JsonMapper.builder().build());
    }


    @DisplayName("then nothing is logged when the service is created")
    @Test
    void testNothingLoggedOnCreate() {
        assertNotNull(auditService);
        verifyNoInteractions(auditLogger);
    }

    @DisplayName("then the start verification request is logged with the right audit metadata")
    @Test
    void logsStartVerificationRequest() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        ClientApplication clientApplication = clientApplication();
        DcqlQuery dcqlQuery = new DcqlQuery();
        URI redirectUri = URI.create("https://client.example/callback");
        StartVerificationRequest request = new StartVerificationRequest(dcqlQuery, redirectUri);

        auditService.auditStartVerificationRequest(request, clientApplication, "tx-123", "request-456");

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertAll(
                () -> assertEquals(AuditID.START_VERIFICATION_REQUEST.auditIdentifier().auditId(), actualEntry.getAuditId().auditId()),
                () -> assertEquals("tx-123", actualEntry.getAttributes().get(AuditService.VERIFIER_TRANSACTION_ID)),
                () -> assertEquals("request-456", actualEntry.getAttributes().get(AuditService.REQUEST_ID)),
                () -> assertEquals("client-123", actualEntry.getAttributes().get(AuditService.CLIENT_APPLICATION_ID)),
                () -> assertSame(dcqlQuery, actualEntry.getAttributes().get(AuditService.DCQL_QUERY)),
                () -> assertEquals(redirectUri.toString(), actualEntry.getAttributes().get(AuditService.REDIRECT_URI))
        );
    }

    @DisplayName("then the start verification response is logged with the right audit metadata")
    @Test
    void logsStartVerificationResponse() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        ClientApplication clientApplication = clientApplication();
        StartVerificationResponse response = new StartVerificationResponse(
                URI.create("eudi-openid4vp://request"),
                URI.create("data:image/png;base64,abc"),
                "tx-123");

        auditService.auditStartVerificationRequestResponse(response, clientApplication, "request-456");

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertAll(
                () -> assertEquals(AuditID.START_VERIFICATION_RESPONSE.auditIdentifier().auditId(), actualEntry.getAuditId().auditId()),
                () -> assertEquals("tx-123", actualEntry.getAttributes().get(AuditService.VERIFIER_TRANSACTION_ID)),
                () -> assertEquals("request-456", actualEntry.getAttributes().get(AuditService.REQUEST_ID)),
                () -> assertEquals("client-123", actualEntry.getAttributes().get(AuditService.CLIENT_APPLICATION_ID)),
                () -> assertEquals(response.authorizationRequest().toString(), actualEntry.getAttributes().get(AuditService.AUTHORIZATION_REQUEST)),
                () -> assertEquals(response.authorizationRequestQRCode().toString(), actualEntry.getAttributes().get(AuditService.AUTHORIZATION_REQUEST_QR_CODE))
        );
    }

    @DisplayName("then the verification data request is logged with the right audit metadata")
    @Test
    void logsRetrieveVerificationDataRequest() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        ClientApplication clientApplication = clientApplication();
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        DcqlQuery dcqlQuery = new DcqlQuery();
        verificationTransaction.setDcqlQuery(dcqlQuery);
        verificationTransaction.setRedirectUri(URI.create("https://client.example/callback"));

        auditService.auditRetrieveVerificationDataRequest(verificationTransaction, "tx-123", clientApplication);

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertAll(
                () -> assertEquals(AuditID.RETRIEVE_VERIFICATION_DATA_REQUEST.auditIdentifier().auditId(), actualEntry.getAuditId().auditId()),
                () -> assertEquals("tx-123", actualEntry.getAttributes().get(AuditService.VERIFIER_TRANSACTION_ID)),
                () -> assertEquals("client-123", actualEntry.getAttributes().get(AuditService.CLIENT_APPLICATION_ID)),
                () -> assertSame(dcqlQuery, actualEntry.getAttributes().get(AuditService.DCQL_QUERY)),
                () -> assertEquals("https://client.example/callback", actualEntry.getAttributes().get(AuditService.REDIRECT_URI))
        );
    }

    @DisplayName("then the verification data response is logged with the right audit metadata")
    @Test
    void logsRetrieveVerificationDataResponse() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        ClientApplication clientApplication = clientApplication();
        VerificationResultResponse response = new VerificationResultResponse("tx-123", Map.of(), Map.of(), Map.of());

        auditService.auditRetrieveVerificationDataResponse(response, "tx-123", clientApplication);

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertAll(
                () -> assertEquals(AuditID.RETRIEVE_VERIFICATION_DATA_RESPONSE.auditIdentifier().auditId(), actualEntry.getAuditId().auditId()),
                () -> assertEquals("tx-123", actualEntry.getAttributes().get(AuditService.VERIFIER_TRANSACTION_ID)),
                () -> assertEquals("client-123", actualEntry.getAttributes().get(AuditService.CLIENT_APPLICATION_ID)),
                () -> assertSame(response.credentials(), actualEntry.getAttributes().get(AuditService.CREDENTIALS))
        );
    }

    @DisplayName("then the authz request retrieval is logged with the right audit metadata")
    @Test
    void logsRetrieveAuthzRequest() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        ClientApplication clientApplication = clientApplication();

        auditService.auditRetrieveAuthzRequest("tx-123", clientApplication, "request-456", "same_device");

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertAll(
                () -> assertEquals(AuditID.RETRIEVE_AUTHZ_REQUEST.auditIdentifier().auditId(), actualEntry.getAuditId().auditId()),
                () -> assertEquals("tx-123", actualEntry.getAttributes().get(AuditService.VERIFIER_TRANSACTION_ID)),
                () -> assertEquals("request-456", actualEntry.getAttributes().get(AuditService.REQUEST_ID)),
                () -> assertEquals("client-123", actualEntry.getAttributes().get(AuditService.CLIENT_APPLICATION_ID)),
                () -> assertEquals("same_device", actualEntry.getAttributes().get(AuditService.FLOW))
        );
    }

    @DisplayName("then the authz request send is logged with the right audit metadata")
    @Test
    void logsSendAuthzRequest() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        ClientApplication clientApplication = clientApplication();
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        verificationTransaction.setFlow("cross_device");

        auditService.auditSendAuthzRequest(verificationTransaction, "tx-123", clientApplication, "request-456", "jwt-token", "state-789");

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertAll(
                () -> assertEquals(AuditID.SEND_AUTHZ_REQUEST.auditIdentifier().auditId(), actualEntry.getAuditId().auditId()),
                () -> assertEquals("tx-123", actualEntry.getAttributes().get(AuditService.VERIFIER_TRANSACTION_ID)),
                () -> assertEquals("request-456", actualEntry.getAttributes().get(AuditService.REQUEST_ID)),
                () -> assertEquals("client-123", actualEntry.getAttributes().get(AuditService.CLIENT_APPLICATION_ID)),
                () -> assertEquals("cross_device", actualEntry.getAttributes().get(AuditService.FLOW)),
                () -> assertEquals("jwt-token", actualEntry.getAttributes().get(AuditService.AUTHORIZATION_REQUEST)),
                () -> assertEquals("state-789", actualEntry.getAttributes().get(AuditService.STATE))
        );
    }

    @DisplayName("then the authz response is logged with the right audit metadata")
    @Test
    void logsReceiveAuthzResponse() {
        ArgumentCaptor<AuditEntry> auditEntry = ArgumentCaptor.forClass(AuditEntry.class);
        ClientApplication clientApplication = clientApplication();
        VpToken vpToken = new VpToken(Map.of("credential-id", List.of(new VerifiablePresentation("presentation-123"))));

        auditService.auditReceiveAuthzResponse(vpToken, "tx-123", clientApplication, "state-789");

        verify(auditLogger).log(auditEntry.capture());
        AuditEntry actualEntry = auditEntry.getValue();
        assertAll(
                () -> assertEquals(AuditID.RECEIVE_AUTHZ_RESPONSE.auditIdentifier().auditId(), actualEntry.getAuditId().auditId()),
                () -> assertEquals("tx-123", actualEntry.getAttributes().get(AuditService.VERIFIER_TRANSACTION_ID)),
                () -> assertEquals("client-123", actualEntry.getAttributes().get(AuditService.CLIENT_APPLICATION_ID)),
                () -> assertEquals("{\"credential-id\":[\"presentation-123\"]}", actualEntry.getAttributes().get(AuditService.VP_TOKEN)),
                () -> assertEquals("state-789", actualEntry.getAttributes().get(AuditService.STATE))
        );
    }

    @DisplayName("then a vp token is converted to JSON for audit logging")
    @Test
    void convertsVpTokenToJson() {
        VpToken vpToken = new VpToken(Map.of("credential-id", List.of(new VerifiablePresentation("presentation-123"))));

        String json = auditService.vpTokenToJson(vpToken);

        assertEquals("{\"credential-id\":[\"presentation-123\"]}", json);
    }

    private static ClientApplication clientApplication() {
        ClientApplication clientApplication = new ClientApplication();
        clientApplication.setId("client-123");
        return clientApplication;
    }

}
