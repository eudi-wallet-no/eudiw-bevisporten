package no.idporten.eudiw.issuer.api.openid4vci;

import io.swagger.v3.oas.annotations.Hidden;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.NotificationRequest;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
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

    private final CredentialIssuerTenantService credentialIssuerTenantService;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;

    public NotificationEndpointController(CredentialIssuerTenantService credentialIssuerTenantService, CredentialIssuanceStatusService credentialIssuanceStatusService, AuthorizationServerService authorizationServerService, AccessTokenValidationService accessTokenValidationService) {
        this.credentialIssuerTenantService = credentialIssuerTenantService;
        this.credentialIssuanceStatusService = credentialIssuanceStatusService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
    }

    @PostMapping(path = {Endpoints.NOTIFICATION_ENDPOINT, Endpoints.NOTIFICATION_ENDPOINT_TENANT}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> notificationEndpoint(
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @RequestBody NotificationRequest notificationRequest,
            @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        // TODO JWT accessToken = https://digdir.atlassian.net/browse/EUW-533
        log.info("Received notification request: {}", notificationRequest);
        log.info("Received notification request authorization header: {}", authorizationHeader);
        CredentialIssuerTenant credentialIssuerTenant = credentialIssuerTenantService.findTenantById(tenant);
        notificationRequest.validate();
        credentialIssuanceStatusService.walletStatusUpdated(new NotificationId(notificationRequest.notificationId()), notificationRequest.event());
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

}
