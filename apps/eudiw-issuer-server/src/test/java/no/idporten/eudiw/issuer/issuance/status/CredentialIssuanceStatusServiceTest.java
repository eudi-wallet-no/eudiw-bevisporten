package no.idporten.eudiw.issuer.issuance.status;


import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionEntity;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionDao;
import no.idporten.eudiw.issuer.credentials.status.persistence.StatusListEntryDao;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("When handling notifications")
@ActiveProfiles("junit")
@SpringBootTest
public class CredentialIssuanceStatusServiceTest {

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

    @DisplayName("then the wallet can update status using notification id and client can poll using issuance transaction id")
    @Test
    void testUpdateAndPollStatus() {
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        JWT accessToken = createIssuanceAccessToken(issuanceTransactionId);
        String cid = "junitdoc_pre_mso_mdoc";
        credentialIssuanceStatusService.offerIssued(credentialIssuerTenantService.findTenantById("root"), issuanceTransactionId, cid);
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(cid, issuanceTransactionId);
        String status = "credential_accepted";
        credentialIssuanceStatusService.walletStatusUpdated(notificationId, status);
        CredentialIssuanceStatus issuanceStatus = credentialIssuanceStatusService.getIssuanceStatus(accessToken,credentialIssuerTenantService.findTenantById("root"), issuanceTransactionId);
        assertAll(
                () -> assertEquals(status, issuanceStatus.status()),
                () -> assertEquals(issuanceTransactionId, issuanceStatus.issuanceTransactionId()),
                () -> assertEquals(cid, issuanceStatus.credentialConfigurationId())
        );
        verify(auditService).logWalletStatusUpdate(eq(cid), eq(issuanceTransactionId), eq(notificationId), eq(status));
    }

    @DisplayName("then an unknown reference has the unknown status")
    @Test
    void unknownReference() {
        assertEquals("unknown", credentialIssuanceStatusService.getIssuanceStatus(null, new CredentialIssuerTenant(), new IssuanceTransactionId()).status());
    }

}
