package no.idporten.eudiw.issuer.openid4vci.service;


import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("When handling notifications")
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class CredentialIssuanceStatusServiceTest {

    @Mock
    private CredentialIssuerServerProperties credentialIssuerServerProperties;

    @Mock
    private AccessTokenValidationService accessTokenValidationService;

    @InjectMocks
    private CredentialIssuanceStatusService credentialIssuanceStatusService;

    @BeforeEach
    void setUp() {
        when(credentialIssuerServerProperties.findCredentialConfiguration(eq("cid"))).thenReturn(
                CredentialConfigurationProperties.builder()
                        .identifier("cid")
                        .scope("test").build());
    }

    private JWT createIssuanceAccessToken(IssuerTransactionId issuerTransactionId) {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer("https://junit.idporten.no")
                .claim("scope", "test")
                .claim("tx_id", issuerTransactionId.getValue())
                .build();
        return new PlainJWT(jwtClaimsSet);
    }

    @DisplayName("then the issuer transaction id references the initial status when the credential offer is issued")
    @Test
    void createStatusWhenOfferIsIssued() {
        IssuerTransactionId issuerTransactionId = new IssuerTransactionId();
        IssuanceStatus issuanceStatus = credentialIssuanceStatusService.offerIssued(issuerTransactionId, "cid");
        assertAll(
                () -> assertEquals(issuerTransactionId, issuanceStatus.issuerTransactionId()),
                () -> assertEquals("cid", issuanceStatus.credentialConfigurationId()),
                () -> assertEquals("offer_issued", issuanceStatus.status())
        );
    }

    @DisplayName("then the notification id references the updated status when credential issues credential")
    @Test
    void updateStatusAndRevealNotificationIdWhenCredentialIsIssued() {
        IssuerTransactionId issuerTransactionId = new IssuerTransactionId();
        JWT accessToken = createIssuanceAccessToken(issuerTransactionId);
        credentialIssuanceStatusService.offerIssued(issuerTransactionId, "cid");
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(accessToken, "cid");
        IssuanceStatus issuanceStatus = credentialIssuanceStatusService.pollIssuerStatus(accessToken, issuerTransactionId);
        assertAll(
                () -> assertEquals("credential_issued", issuanceStatus.status()),
                () -> assertEquals("cid", issuanceStatus.credentialConfigurationId()),
                () -> assertNotNull(notificationId),
                () -> assertNotEquals(issuerTransactionId, notificationId)
        );
    }

    @DisplayName("then the wallet can update status using notification id and client can poll using issuer transaction id")
    @Test
    void testUpdateAndPollStatus() {
        IssuerTransactionId issuerTransactionId = new IssuerTransactionId();
        JWT accessToken = createIssuanceAccessToken(issuerTransactionId);
        credentialIssuanceStatusService.offerIssued(issuerTransactionId, "cid");
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(accessToken, "cid");
        credentialIssuanceStatusService.walletStatusUpdated(notificationId, "credential_accepted");
        IssuanceStatus issuanceStatus = credentialIssuanceStatusService.pollIssuerStatus(accessToken, issuerTransactionId);
        assertAll(
                () -> assertEquals("credential_accepted", issuanceStatus.status()),
                () -> assertEquals(issuerTransactionId, issuanceStatus.issuerTransactionId()),
                () -> assertEquals("cid", issuanceStatus.credentialConfigurationId())
        );
    }

    @DisplayName("then an unknown reference has the unknown status")
    @Test
    void unknownReference() {
        assertEquals("unknown", credentialIssuanceStatusService.pollIssuerStatus(null, new IssuerTransactionId()).status());
    }

}
