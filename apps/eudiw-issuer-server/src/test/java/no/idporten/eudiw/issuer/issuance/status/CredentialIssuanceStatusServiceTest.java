package no.idporten.eudiw.issuer.issuance.status;


import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionEntity;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionDao;
import no.idporten.eudiw.issuer.credentials.status.persistence.StatusListEntryDao;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenCredentialValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("When handling notifications, then validated issuance status updates are expected")
@ActiveProfiles("junit")
@SpringBootTest
public class CredentialIssuanceStatusServiceTest {

    private static final String CREDENTIAL_CONFIGURATION_ID = "junitdoc_pre_mso_mdoc";
    private static final String WALLET_STATUS = "credential_accepted";

    @MockitoBean
    private AccessTokenValidationService accessTokenValidationService;

    @MockitoBean
    private AuditService auditService;

    @Autowired
    private CredentialIssuanceStatusService credentialIssuanceStatusService;

    @Autowired
    private CredentialIssuerTenantService credentialIssuerTenantService;

    @Autowired
    private CredentialIssuanceTransactionDao issuanceTransactionDao;

    @Autowired
    private StatusListEntryDao statusListEntryDao;

    @BeforeEach
    void setUp() {
        statusListEntryDao.deleteAll();
        issuanceTransactionDao.deleteAll();
    }

    private JWT createIssuanceAccessToken(IssuanceTransactionId issuanceTransactionId) {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer("https://junit.idporten.no")
                .claim("scope", "test")
                .claim("tx_id", issuanceTransactionId.getValue())
                .build();
        return new PlainJWT(jwtClaimsSet);
    }

    @DisplayName("then the issuance transaction id references the initial status when the credential offer is issued")
    @Test
    void createStatusWhenOfferIsIssued() {
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        String credConfigId = "junitdoc_pre_mso_mdoc";
        CredentialIssuanceStatus issuanceStatus = credentialIssuanceStatusService.offerIssued(credentialIssuerTenantService.findTenantById("root"), issuanceTransactionId, credConfigId);
        CredentialIssuanceTransactionEntity persisted = issuanceTransactionDao.findTransaction(
                issuanceTransactionId.getValue(),
                credConfigId,
                "root"
        ).orElseThrow();
        assertAll(
                () -> assertEquals(issuanceTransactionId, issuanceStatus.issuanceTransactionId()),
                () -> assertEquals(credConfigId, issuanceStatus.credentialConfigurationId()),
                () -> assertEquals("offer_issued", issuanceStatus.status()),
                () -> assertEquals(credConfigId, persisted.getCredentialConfigurationId()),
                () -> assertEquals("root", persisted.getCredentialIssuerTenant()),
                () -> assertEquals("offer_issued", persisted.getStatus())
        );
    }

    @DisplayName("then the notification id references the updated status when credential issues credential")
    @Test
    void updateStatusAndRevealNotificationIdWhenCredentialIsIssued() {
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        String credConfigId = "junitdoc_pre_mso_mdoc";
        JWT accessToken = createIssuanceAccessToken(issuanceTransactionId);
        credentialIssuanceStatusService.offerIssued(credentialIssuerTenantService.findTenantById("root"), issuanceTransactionId, credConfigId);
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(credConfigId, issuanceTransactionId);
        CredentialIssuanceStatus issuanceStatus = credentialIssuanceStatusService.getIssuanceStatus(accessToken, credentialIssuerTenantService.findTenantById("root"), issuanceTransactionId);
        CredentialIssuanceTransactionEntity persisted = issuanceTransactionDao.findByNotificationId(notificationId.getValue()).orElseThrow();
        assertAll(
                () -> assertEquals("credential_issued", issuanceStatus.status()),
                () -> assertEquals(credConfigId, issuanceStatus.credentialConfigurationId()),
                () -> assertNotNull(notificationId),
                () -> assertNotEquals(issuanceTransactionId, notificationId),
                () -> assertEquals(issuanceTransactionId.getValue(), persisted.getIssuanceTransactionId()),
                () -> assertEquals(credConfigId, persisted.getCredentialConfigurationId()),
                () -> assertEquals("credential_issued", persisted.getStatus())
        );
    }

    @DisplayName("When a wallet updates status, then validation against the transaction's credential configuration is expected")
    @ParameterizedTest
    @CsvSource({
            "root, junitdoc_pre_mso_mdoc",
            "junit, junitdoc_pre_sd_jwt_vc"
    })
    void testUpdateAndPollStatus(String tenantId, String cid) {
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        JWT accessToken = createIssuanceAccessToken(issuanceTransactionId);
        CredentialIssuerTenant tenant = credentialIssuerTenantService.findTenantById(tenantId);
        credentialIssuanceStatusService.offerIssued(tenant, issuanceTransactionId, cid);
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(cid, issuanceTransactionId);
        credentialIssuanceStatusService.walletStatusUpdated(tenant, notificationId, WALLET_STATUS, accessToken);
        CredentialIssuanceStatus issuanceStatus = credentialIssuanceStatusService.getIssuanceStatus(accessToken, tenant, issuanceTransactionId);
        assertAll(
                () -> assertEquals(WALLET_STATUS, issuanceStatus.status()),
                () -> assertEquals(issuanceTransactionId, issuanceStatus.issuanceTransactionId()),
                () -> assertEquals(cid, issuanceStatus.credentialConfigurationId())
        );
        verify(accessTokenValidationService).validateAccessTokenForCredentialConfiguration(
                same(accessToken), eq(AccessTokenCredentialValidationContext.forAuthorization(tenant.findCredentialConfiguration(cid))));
        verify(auditService).logWalletStatusUpdate(eq(cid), eq(issuanceTransactionId), eq(notificationId), eq(WALLET_STATUS));
    }

    @DisplayName("When credential token validation fails, then unchanged status and no audit event are expected")
    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"INVALID_TOKEN", "INSUFFICIENT_SCOPE"})
    void testRejectedWalletStatusUpdate(ErrorCode errorCode) {
        CredentialIssuerTenant tenant = credentialIssuerTenantService.findTenantById("root");
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        JWT accessToken = createIssuanceAccessToken(issuanceTransactionId);
        credentialIssuanceStatusService.offerIssued(tenant, issuanceTransactionId, CREDENTIAL_CONFIGURATION_ID);
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(CREDENTIAL_CONFIGURATION_ID, issuanceTransactionId);
        CredentialIssuanceTransactionEntity before = issuanceTransactionDao.findByNotificationId(notificationId.getValue()).orElseThrow();
        IssuerServerException validationFailure = new IssuerServerException(errorCode, "Token does not authorize this credential configuration.");
        doThrow(validationFailure).when(accessTokenValidationService).validateAccessTokenForCredentialConfiguration(
                same(accessToken), eq(AccessTokenCredentialValidationContext.forAuthorization(tenant.findCredentialConfiguration(CREDENTIAL_CONFIGURATION_ID))));

        assertSame(validationFailure, assertThrows(IssuerServerException.class,
                () -> credentialIssuanceStatusService.walletStatusUpdated(tenant, notificationId, WALLET_STATUS, accessToken)));
        CredentialIssuanceTransactionEntity after = issuanceTransactionDao.findByNotificationId(notificationId.getValue()).orElseThrow();
        assertEquals(before.getStatus(), after.getStatus());
        assertEquals(before.getUpdatedMs(), after.getUpdatedMs());
        verifyNoInteractions(auditService);
    }

    @DisplayName("When a notification belongs to another tenant, then rejection without a status update is expected")
    @Test
    void testNotificationForAnotherTenant() {
        CredentialIssuerTenant tenant = credentialIssuerTenantService.findTenantById("root");
        CredentialIssuerTenant otherTenant = credentialIssuerTenantService.findTenantById("junit");
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        JWT accessToken = createIssuanceAccessToken(issuanceTransactionId);
        String credentialConfigurationId = "junitdoc_pre_sd_jwt_vc";
        credentialIssuanceStatusService.offerIssued(tenant, issuanceTransactionId, credentialConfigurationId);
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(credentialConfigurationId, issuanceTransactionId);

        IssuerServerException exception = assertThrows(IssuerServerException.class,
                () -> credentialIssuanceStatusService.walletStatusUpdated(otherTenant, notificationId, WALLET_STATUS, accessToken));

        assertEquals("invalid_notification_id", exception.getError());
        assertEquals("credential_issued", issuanceTransactionDao.findByNotificationId(notificationId.getValue()).orElseThrow().getStatus());
        verifyNoInteractions(accessTokenValidationService, auditService);
    }

    @DisplayName("When a notification is unknown, then an invalid notification id error is expected")
    @Test
    void testUnknownNotification() {
        IssuerServerException exception = assertThrows(IssuerServerException.class,
                () -> credentialIssuanceStatusService.walletStatusUpdated(credentialIssuerTenantService.findTenantById("root"), new NotificationId(), WALLET_STATUS, createIssuanceAccessToken(new IssuanceTransactionId())
                ));
        assertEquals("invalid_notification_id", exception.getError());
        assertEquals(400, exception.getHttpStatus().value());
        verifyNoInteractions(accessTokenValidationService, auditService);
    }

    @DisplayName("When a transaction has no status, then a validated status update is expected")
    @Test
    void testNotificationWithoutStatus() {
        CredentialIssuerTenant tenant = credentialIssuerTenantService.findTenantById("root");
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        NotificationId notificationId = new NotificationId();
        JWT accessToken = createIssuanceAccessToken(issuanceTransactionId);
        issuanceTransactionDao.insertTransaction(issuanceTransactionId.getValue(), CREDENTIAL_CONFIGURATION_ID, tenant.getId(), System.currentTimeMillis());
        issuanceTransactionDao.updateNotificationId(issuanceTransactionId.getValue(), notificationId.getValue(), System.currentTimeMillis());

        credentialIssuanceStatusService.walletStatusUpdated(tenant, notificationId, WALLET_STATUS, accessToken);

        assertEquals(WALLET_STATUS, issuanceTransactionDao.findByNotificationId(notificationId.getValue()).orElseThrow().getStatus());
        verify(accessTokenValidationService).validateAccessTokenForCredentialConfiguration(
                same(accessToken), eq(AccessTokenCredentialValidationContext.forAuthorization(tenant.findCredentialConfiguration(CREDENTIAL_CONFIGURATION_ID))));
        verify(auditService).logWalletStatusUpdate(CREDENTIAL_CONFIGURATION_ID, issuanceTransactionId, notificationId, WALLET_STATUS);
    }

    @DisplayName("then an unknown reference has the unknown status")
    @Test
    void unknownReference() {
        assertEquals("unknown", credentialIssuanceStatusService.getIssuanceStatus(null, new CredentialIssuerTenant(), new IssuanceTransactionId()).status());
    }

}
