package no.idporten.eudiw.issuer.api.openid4vci;

import com.nimbusds.jwt.JWT;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.eudiw.issuer.openid4vci.protocol.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            HttpServletRequest request) {
        CredentialIssuerTenant credentialIssuerTenant = credentialIssuerTenantService.findTenantById(tenant);
        JWT accessToken = accessTokenValidationService.validateAccessToken(AccessTokenValidationContext.forDPoPToken(request, List.of(authorizationServerService.getPrimaryAuthorizationServer()), credentialIssuerTenant.getCredentialIssuer()));
        notificationRequest.validate();
        log.info("Received notification request: {}", notificationRequest);
        credentialIssuanceStatusService.walletStatusUpdated(credentialIssuerTenant, new NotificationId(notificationRequest.notificationId()), notificationRequest.event(), accessToken);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

}
