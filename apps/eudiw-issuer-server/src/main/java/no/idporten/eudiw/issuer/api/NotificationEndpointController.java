package no.idporten.eudiw.issuer.api;

import io.swagger.v3.oas.annotations.Hidden;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.NotificationRequest;
import no.idporten.eudiw.issuer.openid4vci.service.IssuerTransactionId;
import no.idporten.eudiw.issuer.openid4vci.service.NotificationId;
import no.idporten.eudiw.issuer.openid4vci.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
public class NotificationEndpointController {

    final Logger log = LoggerFactory.getLogger(NotificationEndpointController.class);

    private final NotificationService notificationService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;

    public NotificationEndpointController(NotificationService notificationService, AuthorizationServerService authorizationServerService, AccessTokenValidationService accessTokenValidationService) {
        this.notificationService = notificationService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
    }

    @PostMapping(path = Endpoints.NOTIFICATION_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> notificationEndpoint(@RequestBody NotificationRequest notificationRequest,
                                                                   @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        // TODO JWT accessToken = accessTokenValidationService.validateAccessTokenForCredentialConfiguration(authorizationHeader, List.of(authorizationServerService.getPrimaryAuthorizationServer()));
        log.info("Received notification request: {}", notificationRequest);
        log.info("Received notification request authorization header: {}", authorizationHeader);
        notificationRequest.validate();
        notificationService.walletStatusUpdated(new NotificationId(notificationRequest.notificationId()), notificationRequest.event());
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    // TODO egen controller under API v1 og beskyttelse - hva er hensiktsmessig?  egen sak
    @Hidden
    @GetMapping(path = Endpoints.NOTIFICATION_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> pollEndpoint(@RequestParam(name = "issuer_transaction_id") String issuerTransactionId) {
        String issuanceStatus = notificationService.pollIssuerStatus(new IssuerTransactionId(issuerTransactionId));
        return ResponseEntity.ok(issuanceStatus);
    }

}
