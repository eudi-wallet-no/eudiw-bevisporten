package no.idporten.eudiw.issuer.credentials.status;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.context.CredentialRevokeContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.cache.CredentialStatusInfo;
import no.idporten.eudiw.issuer.credentials.status.cache.InMemoryCredentialStatusCache;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusIssuerIntegration;
import no.idporten.eudiw.issuer.credentials.status.integration.UpdatedStatusEntry;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
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

    @Spy
    InMemoryCredentialStatusCache credentialStatusCache;

    @Captor
    ArgumentCaptor<CredentialStatusInfo> credentialStatusInfoCaptor;

    @Captor
    ArgumentCaptor<List<UpdatedStatusEntry>> statusEntryListCaptor;

    private CredentialIssueContext testContext() {
        return new CredentialIssueContext(
                preAuthAccessToken(syntheticPersonIdentifier(), new IssuanceTransactionId()),
                junitIssuerTenant(),
                junitCredentialConfiguration());
    }

    @BeforeEach
    void setUp() {
        reset(statusIssuerIntegration, credentialStatusCache);
    }

    @DisplayName("then the status list feature is feature switched and applies only for pre-authorized code flow")
    @Test
    void testFeatureSwitch() {
        StatusIssuerProperties properties = new StatusIssuerProperties();
        StatusIssuerService service = new StatusIssuerService(properties, null, credentialStatusCache);
        CredentialIssueContext context = testContext();
        assertFalse(service.isEnabled(context));
        properties.setEnabled(true);
        assertTrue(service.isEnabled(context));
        verifyNoInteractions(statusIssuerIntegration, credentialStatusCache);
    }

    @DisplayName("then status entries is allocated by integrating with the status issuer")
    @Test
    void testAllocateStatus() {
        StatusIssuerService service = new StatusIssuerService(null, statusIssuerIntegration, credentialStatusCache);
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
        verify(credentialStatusCache).storeCredentialStatus(eq(junitIssuerTenant()), any(), credentialStatusInfoCaptor.capture(), any());
        CredentialStatusInfo credentialStatusInfo = credentialStatusInfoCaptor.getValue();
        assertEquals(2, credentialStatusInfo.statusEntries().size());
    }

    @DisplayName("then revoking a credential will update the status issuer")
    @Test
    void testRevoke() {
        StatusIssuerService service = new StatusIssuerService(null, statusIssuerIntegration, credentialStatusCache);
        CredentialIssuerTenant tenant = junitIssuerTenant();
        ExtendedCredentialConfiguration credentialConfiguration = junitCredentialConfiguration();
        IssuanceTransactionId transactionId = new IssuanceTransactionId();
        CredentialStatusInfo credentialStatusInfo = new CredentialStatusInfo(tenant.getId(), credentialConfiguration.getCredentialConfigurationId(), List.of(new StatusEntry(7, URI.create("https://junit.eidas2sandkasse.dev/lists/0"))));
        credentialStatusCache.storeCredentialStatus(tenant, transactionId, credentialStatusInfo, Duration.ofMinutes(1));
        CredentialRevokeContext context = new CredentialRevokeContext(preAuthAccessToken(syntheticPersonIdentifier(), transactionId), tenant, credentialConfiguration, transactionId);
        service.revokeStatus(context);
        verify(statusIssuerIntegration).updateStatusEntries(statusEntryListCaptor.capture());
        List<UpdatedStatusEntry> updatedStatusEntries = statusEntryListCaptor.getValue();
        assertAll(
                () -> assertEquals(1, updatedStatusEntries.size()),
                () -> assertEquals(7, updatedStatusEntries.getFirst().idx()),
                () -> assertEquals("https://junit.eidas2sandkasse.dev/lists/0", updatedStatusEntries.getFirst().uri().toString()),
                () -> assertEquals("INVALID", updatedStatusEntries.getFirst().statusType())
        );
    }

    @DisplayName("then revoking with a non-matching credential configuration id is not allowed")
    @Test
    void testRevokeWithInvalidCredentialConfigurationId() {
        StatusIssuerService service = new StatusIssuerService(null, statusIssuerIntegration, credentialStatusCache);
        CredentialIssuerTenant tenant = junitIssuerTenant();
        ExtendedCredentialConfiguration credentialConfiguration = junitCredentialConfiguration();
        IssuanceTransactionId transactionId = new IssuanceTransactionId();
        CredentialStatusInfo credentialStatusInfo = new CredentialStatusInfo(tenant.getId(), "somethingelse", List.of(new StatusEntry(7, URI.create("https://junit.eidas2sandkasse.dev/lists/0"))));
        credentialStatusCache.storeCredentialStatus(tenant, transactionId, credentialStatusInfo, Duration.ofMinutes(1));
        CredentialRevokeContext context = new CredentialRevokeContext(preAuthAccessToken(syntheticPersonIdentifier(), transactionId), tenant, credentialConfiguration, transactionId);
        IssuerServerException exception = assertThrows(IssuerServerException.class, () -> service.revokeStatus(context));
        assertTrue(exception.getErrorDescription().contains("Not allowed to revoke credential status"));
    }

    @DisplayName("then revoking an unknown credential does not update the status issuer")
    @Test
    void testRevokeUnknownCredential() {
        StatusIssuerService service = new StatusIssuerService(null, statusIssuerIntegration, credentialStatusCache);
        CredentialIssuerTenant tenant = junitIssuerTenant();
        ExtendedCredentialConfiguration credentialConfiguration = junitCredentialConfiguration();
        IssuanceTransactionId transactionId = new IssuanceTransactionId();
        CredentialRevokeContext context = new CredentialRevokeContext(preAuthAccessToken(syntheticPersonIdentifier(), transactionId), tenant, credentialConfiguration, transactionId);
        service.revokeStatus(context);
        verifyNoInteractions(statusIssuerIntegration);
    }

}
