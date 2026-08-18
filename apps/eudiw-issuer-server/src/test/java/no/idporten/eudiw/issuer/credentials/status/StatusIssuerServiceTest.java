package no.idporten.eudiw.issuer.credentials.status;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.context.CredentialRevokeContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusIssuerIntegration;
import no.idporten.eudiw.issuer.credentials.status.integration.UpdatedStatusEntry;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialStatusService;
import no.idporten.eudiw.issuer.issuance.CredentialIssuanceType;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;

import static no.idporten.eudiw.issuer.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("When allocation a status on the status list")
@ExtendWith(MockitoExtension.class)
public class StatusIssuerServiceTest {

    @Mock
    StatusIssuerIntegration statusIssuerIntegration;

    @Mock
    AuditService auditService;

    @Mock
    CredentialStatusService credentialStatusService;

    @Mock
    StatusIssuerProperties statusIssuerProperties;

    @InjectMocks
    StatusIssuerService service;

    @Captor
    ArgumentCaptor<CredentialStatusInfo> credentialStatusInfoCaptor;

    @Captor
    ArgumentCaptor<List<UpdatedStatusEntry>> updatedStatusEntryListCaptor;

    @Captor
    ArgumentCaptor<List<StatusEntry>> statusEntryListCaptor;

    private CredentialIssueContext testContext() {
        IssuanceTransactionId transactionId = new IssuanceTransactionId();
        return new CredentialIssueContext(
                preAuthAccessToken(syntheticPersonIdentifier(), transactionId),
                junitIssuerTenant(),
                junitCredentialConfiguration(),
                transactionId,
                CredentialIssuanceType.PRE_AUTHORIZED_CODE);
    }

    private CredentialIssueContext authorizationCodeContext() {
        IssuanceTransactionId transactionId = new IssuanceTransactionId();
        return new CredentialIssueContext(
                accessToken(syntheticPersonIdentifier()),
                junitIssuerTenant(),
                junitCredentialConfiguration(),
                transactionId,
                CredentialIssuanceType.AUTHORIZATION_CODE);
    }

    @BeforeEach
    void setUp() {
        reset(statusIssuerIntegration, credentialStatusService);
    }

    @DisplayName("then the status list feature is feature switched and is per credential configuration")
    @Test
    void testFeatureSwitch() {
        StatusIssuerProperties properties = new StatusIssuerProperties();
        StatusIssuerService service = new StatusIssuerService(properties, null, credentialStatusService, auditService);
        CredentialIssueContext context = testContext();
        context.credentialConfiguration().getCredentialIssuerContext().setIncludeStatus(true);
        assertFalse(service.isEnabled(context));
        properties.setEnabled(true);
        assertTrue(service.isEnabled(context));
        verifyNoInteractions(statusIssuerIntegration, credentialStatusService);
    }

    @DisplayName("then feature switch also applies to authorization code flow")
    @Test
    void testFeatureSwitchForAuthorizationCodeFlow() {
        StatusIssuerProperties properties = new StatusIssuerProperties();
        StatusIssuerService service = new StatusIssuerService(properties, null, credentialStatusService, auditService);
        CredentialIssueContext context = authorizationCodeContext();
        context.credentialConfiguration().getCredentialIssuerContext().setIncludeStatus(true);
        assertFalse(service.isEnabled(context));
        properties.setEnabled(true);
        assertTrue(service.isEnabled(context));
        verifyNoInteractions(statusIssuerIntegration, credentialStatusService);
    }

    @DisplayName("then status entries is allocated by integrating with the status issuer")
    @Test
    void testAllocateStatus() {
        when(statusIssuerIntegration.allocateStatusEntries(anyInt()))
                .thenAnswer(invocationOnMock -> IntStream.range(0, invocationOnMock.getArgument(0))
                        .mapToObj(idx -> new StatusEntry(idx, URI.create("https://junit.eidas2sandkasse.dev/lists/" + idx * 2)))
                        .toList());
        List<CredentialStatus> credentialStatus = service.allocateStatus(testContext(), 2);
        assertAll(
                () -> assertEquals(2, credentialStatus.size()),
                () -> assertEquals(0, credentialStatus.getFirst().statusList().index()),
                () -> assertEquals("https://junit.eidas2sandkasse.dev/lists/0", credentialStatus.getFirst().statusList().uri().toString()),
                () -> assertEquals(1, credentialStatus.getLast().statusList().index()),
                () -> assertEquals("https://junit.eidas2sandkasse.dev/lists/2", credentialStatus.getLast().statusList().uri().toString())
        );
        verify(credentialStatusService).storeCredentialStatus(eq(junitIssuerTenant()), any(), credentialStatusInfoCaptor.capture());
        CredentialStatusInfo credentialStatusInfo = credentialStatusInfoCaptor.getValue();
        assertEquals(2, credentialStatusInfo.statusEntries().size());
        verify(auditService).logIssueCredentialStatus(any(), statusEntryListCaptor.capture());
        assertEquals(2, statusEntryListCaptor.getValue().size());
    }

    @DisplayName("then revoking a credential will update the status issuer")
    @Test
    void testRevoke() {
        CredentialIssuerTenant tenant = junitIssuerTenant();
        ExtendedCredentialConfiguration credentialConfiguration = junitCredentialConfiguration();
        IssuanceTransactionId transactionId = new IssuanceTransactionId();
        CredentialStatusInfo credentialStatusInfo = new CredentialStatusInfo(tenant.getId(), credentialConfiguration.getCredentialConfigurationId(), List.of(new StatusEntry(7, URI.create("https://junit.eidas2sandkasse.dev/lists/0"))));
        when(credentialStatusService.retrieveCredentialStatus(tenant, transactionId)).thenReturn(credentialStatusInfo);
        CredentialRevokeContext context = new CredentialRevokeContext(preAuthAccessToken(syntheticPersonIdentifier(), transactionId), tenant, credentialConfiguration, transactionId);
        service.revokeStatus(context);
        verify(statusIssuerIntegration).updateStatusEntries(updatedStatusEntryListCaptor.capture());
        List<UpdatedStatusEntry> updatedStatusEntries = updatedStatusEntryListCaptor.getValue();
        assertAll(
                () -> assertEquals(1, updatedStatusEntries.size()),
                () -> assertEquals(7, updatedStatusEntries.getFirst().idx()),
                () -> assertEquals("https://junit.eidas2sandkasse.dev/lists/0", updatedStatusEntries.getFirst().uri().toString()),
                () -> assertEquals("INVALID", updatedStatusEntries.getFirst().statusType())
        );
        verify(auditService).logRevokeCredential(any(), statusEntryListCaptor.capture(), eq("INVALID"));
        assertEquals(1, statusEntryListCaptor.getValue().size());
        verify(credentialStatusService).markCredentialRevoked(tenant, transactionId);
    }

    @DisplayName("then revoking with a non-matching credential configuration id is not allowed")
    @Test
    void testRevokeWithInvalidCredentialConfigurationId() {
        CredentialIssuerTenant tenant = junitIssuerTenant();
        ExtendedCredentialConfiguration credentialConfiguration = junitCredentialConfiguration();
        IssuanceTransactionId transactionId = new IssuanceTransactionId();
        CredentialStatusInfo credentialStatusInfo = new CredentialStatusInfo(tenant.getId(), "somethingelse", List.of(new StatusEntry(7, URI.create("https://junit.eidas2sandkasse.dev/lists/0"))));
        when(credentialStatusService.retrieveCredentialStatus(tenant, transactionId)).thenReturn(credentialStatusInfo);
        CredentialRevokeContext context = new CredentialRevokeContext(preAuthAccessToken(syntheticPersonIdentifier(), transactionId), tenant, credentialConfiguration, transactionId);
        IssuerServerException exception = assertThrows(IssuerServerException.class, () -> service.revokeStatus(context));
        assertTrue(exception.getErrorDescription().contains("Not allowed to revoke credential status"));
    }

    @DisplayName("then revoking an unknown credential does not update the status issuer")
    @Test
    void testRevokeUnknownCredential() {
        CredentialIssuerTenant tenant = junitIssuerTenant();
        ExtendedCredentialConfiguration credentialConfiguration = junitCredentialConfiguration();
        IssuanceTransactionId transactionId = new IssuanceTransactionId();
        when(credentialStatusService.retrieveCredentialStatus(tenant, transactionId)).thenReturn(null);
        CredentialRevokeContext context = new CredentialRevokeContext(preAuthAccessToken(syntheticPersonIdentifier(), transactionId), tenant, credentialConfiguration, transactionId);
        service.revokeStatus(context);
        verifyNoInteractions(statusIssuerIntegration);
    }

    @DisplayName("then allocateStatus throws NullPointerException when transaction id is null")
    @Test
    void testAllocateStatusThrowsNullPointerExceptionWhenTransactionIdIsNull() {
        CredentialIssueContext context = mock(CredentialIssueContext.class);
        when(context.transactionId()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> service.allocateStatus(context, 1));
    }

}
