package no.idporten.eudiw.issuer.openid4vci.service;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling notifications")
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @DisplayName("then the issuer transaction id references the initial status when the credential offer is issued")
    @Test
    void createStatusWhenOfferIsIssued() {
        IssuerTransactionId issuerTransactionId = new IssuerTransactionId();
        notificationService.offerIssued(issuerTransactionId);
        assertEquals("offer_issued", notificationService.pollIssuerStatus(issuerTransactionId));
    }

    @DisplayName("then the notification id references the updated status when credential issues credential")
    @Test
    void updateStatusAndRevealNotiicationIdWhenCredentialIsIssed() {
        IssuerTransactionId issuerTransactionId = new IssuerTransactionId();
        notificationService.offerIssued(issuerTransactionId);
        NotificationId notificationId = notificationService.credentialIssued(issuerTransactionId);
        assertAll(
                () -> assertEquals("credential_issued", notificationService.pollIssuerStatus(issuerTransactionId)),
                () -> assertNotNull(notificationId),
                () -> assertNotEquals(issuerTransactionId, notificationId)
        );
    }

    @DisplayName("then the wallet can update status using notification id and client can poll using issuer transaction id")
    @Test
    void testUpdateAndPollStatus() {
        IssuerTransactionId issuerTransactionId = new IssuerTransactionId();
        notificationService.offerIssued(issuerTransactionId);
        NotificationId notificationId = notificationService.credentialIssued(issuerTransactionId);
        notificationService.walletStatusUpdated(notificationId, "credential_accepted");
        assertEquals("credential_accepted", notificationService.pollIssuerStatus(issuerTransactionId));
    }

    @DisplayName("then an unknown reference has the unknown status")
    @Test
    void unknownReference() {
        assertEquals("unknown", notificationService.pollIssuerStatus(new IssuerTransactionId()));
    }

}
