package no.idporten.eudiw.login.openid4vp;

import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;


@Controller
public class OpenID4VPController {

    private final OpenID4VPService openID4VPService;

    private final WalletInteractionService walletInteractionService;

    Logger logger = org.slf4j.LoggerFactory.getLogger(OpenID4VPController.class);

    public OpenID4VPController(OpenID4VPService openID4VPService, WalletInteractionService walletInteractionService) {
        this.openID4VPService = openID4VPService;
        this.walletInteractionService = walletInteractionService;
    }

    @GetMapping(path = "/openid4vp/request/{flow}/{id}", produces = "application/oauth-authz-req+jwt")
    public ResponseEntity<String> vpRequestEndpoint(@PathVariable OpenID4VPFlow flow, @PathVariable String id) throws Exception {
        logger.debug("vpRequestEndpoint id: {}", id);
        SignedJWT signedRequestObject = openID4VPService.createPresentationRequest(flow, id);
        WalletInteraction walletInteraction = walletInteractionService.getWalletInteraction(id);
        walletInteraction.setFlow(flow);
        walletInteractionService.updateWalletInteraction(walletInteraction);
        return ResponseEntity.ok(signedRequestObject.serialize());
    }


    @PostMapping(path = "/openid4vp/response/{flow}/{id}", consumes = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SameDeviceResponse> vpResponseEndpoint(@PathVariable OpenID4VPFlow flow, @PathVariable String id, EncryptedAuthorizationResponse response) throws Exception {
        logger.debug("vpResponseEndpoint id: {}", id);
        Map<String, String> claims = openID4VPService.handleWalletResponse(response.response());
        WalletInteraction walletInteraction = walletInteractionService.getWalletInteraction(id);
        if (walletInteraction == null) {
            return ResponseEntity.badRequest().build();
        }
        walletInteraction.setPersonIdentifier(claims.get("personal_administrative_number"));
        walletInteractionService.updateWalletInteraction(walletInteraction);
        String redirectUri = openID4VPService.createRedirectURI(flow, id).toString();
        return switch (flow) {
            case same_device -> {
                yield ResponseEntity.ok().body(new SameDeviceResponse(redirectUri));
            }
            default -> ResponseEntity.ok().build();
        };
    }

}
