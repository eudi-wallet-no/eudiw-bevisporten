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
        IssuanceStatus issuanceStatus = credentialIssuanceStatusService.offerIssued(issuanceTransactionId, "cid");
        assertAll(
                () -> assertEquals(issuanceTransactionId, issuanceStatus.issuanceTransactionId()),
                () -> assertEquals("cid", issuanceStatus.credentialConfigurationId()),
                () -> assertEquals("offer_issued", issuanceStatus.status())
        );
    }

    @DisplayName("then the notification id references the updated status when credential issues credential")
    @Test
    void updateStatusAndRevealNotificationIdWhenCredentialIsIssued() {
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        JWT accessToken = createIssuanceAccessToken(issuanceTransactionId);
        credentialIssuanceStatusService.offerIssued(issuanceTransactionId, "cid");
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(accessToken, "cid");
        IssuanceStatus issuanceStatus = credentialIssuanceStatusService.getIssuanceStatus(accessToken, issuanceTransactionId);
        assertAll(
                () -> assertEquals("credential_issued", issuanceStatus.status()),
                () -> assertEquals("cid", issuanceStatus.credentialConfigurationId()),
                () -> assertNotNull(notificationId),
                () -> assertNotEquals(issuanceTransactionId, notificationId)
        );
    }

    @DisplayName("then the wallet can update status using notification id and client can poll using issuance transaction id")
    @Test
    void testUpdateAndPollStatus() {
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        JWT accessToken = createIssuanceAccessToken(issuanceTransactionId);
        credentialIssuanceStatusService.offerIssued(issuanceTransactionId, "cid");
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(accessToken, "cid");
        credentialIssuanceStatusService.walletStatusUpdated(notificationId, "credential_accepted");
        IssuanceStatus issuanceStatus = credentialIssuanceStatusService.getIssuanceStatus(accessToken, issuanceTransactionId);
        assertAll(
                () -> assertEquals("credential_accepted", issuanceStatus.status()),
                () -> assertEquals(issuanceTransactionId, issuanceStatus.issuanceTransactionId()),
                () -> assertEquals("cid", issuanceStatus.credentialConfigurationId())
        );
    }

    @DisplayName("then an unknown reference has the unknown status")
    @Test
    void unknownReference() {
        assertEquals("unknown", credentialIssuanceStatusService.getIssuanceStatus(null, new IssuanceTransactionId()).status());
    }

}
