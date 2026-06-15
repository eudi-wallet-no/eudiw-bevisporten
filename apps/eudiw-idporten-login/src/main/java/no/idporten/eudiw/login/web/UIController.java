package no.idporten.eudiw.login.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import no.idporten.eudiw.login.openid4vp.OpenID4VPFlow;
import no.idporten.eudiw.login.openid4vp.OpenID4VPService;
import no.idporten.eudiw.login.openid4vp.WalletInteraction;
import no.idporten.eudiw.login.openid4vp.WalletInteractionService;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.awt.image.BufferedImage;
import java.util.UUID;

/**
 * Handle browser interaction: OIDC frontchannel, OpenID4VP frontchannel, UI.
 */
@Controller
public class UIController {

    private static final String SESSION_ATTRIBUTE_REF = "requri";

    private final OpenIDConnectIntegration openIDConnectServer;
    private final OpenID4VPService openID4VPService;
    private final WalletInteractionService walletInteractionService;

    public UIController(OpenIDConnectIntegration openIDConnectServer, OpenID4VPService openID4VPService, WalletInteractionService walletInteractionService) {
        this.openIDConnectServer = openIDConnectServer;
        this.openID4VPService = openID4VPService;
        this.walletInteractionService = walletInteractionService;
    }

    /**
     * Handle OIDC authorization request and redirect to login page.
     */
    @GetMapping("/authorize")
    public String authorize(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, HttpSession session) {
        PushedAuthorizationRequest pushedAuthorizationRequest = openIDConnectServer.process(new AuthorizationRequest(headers, parameters));
        String ref = UUID.randomUUID().toString();
        openIDConnectServer.getSDKConfiguration().getCache().putAuthorizationRequest(ref, pushedAuthorizationRequest);
        String walletInteractionId = UUID.randomUUID().toString();
        session.setAttribute(SESSION_ATTRIBUTE_REF , ref);
        return "redirect:/login/" + walletInteractionId;
    }

    /**
     * Render login page and start polling for wallet result.
     */
    @GetMapping(path = "/login/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String login(@PathVariable String id, Model model, HttpSession session) {
        // sjekk om har ein session (ref cookie)
        String ref = (String) session.getAttribute(SESSION_ATTRIBUTE_REF);
        final WalletInteraction walletInteraction = walletInteractionService.getWalletInteraction(id);
        if (walletInteraction == null) {
            walletInteractionService.startWalletInteraction(id);
            com.nimbusds.oauth2.sdk.AuthorizationRequest sameDeviceRequest = openID4VPService.createAuthorizationRequest(OpenID4VPFlow.same_device, id);
            model.addAttribute("sameDeviceRequest", sameDeviceRequest.toURI().toString());
            model.addAttribute("walletInteractionId", id);
            return "login";
        } else {
            PushedAuthorizationRequest pushedAuthorizationRequest = openIDConnectServer.getSDKConfiguration().getCache().getAuthorizationRequest(ref);
            Authorization authorization = Authorization.builder()
                    .sub(walletInteraction.getPersonIdentifier())
                    .acr(pushedAuthorizationRequest.getResolvedAcrValue())
                    .amr("EUDIW")
                    .build();
            AuthorizationResponse authorizationResponse = openIDConnectServer.authorize(pushedAuthorizationRequest, authorization);
            RedirectedResponse response = (RedirectedResponse) openIDConnectServer.createClientResponse(authorizationResponse);

            walletInteractionService.removeWalletInteraction(id);
            // return Response.status(302).location(response.toQueryRedirectUri()).cookie(null).build();
            return "redirect:" + response.toQueryRedirectUri();
        }
    }

    /**
     * Return to calling OIDC client application if user cancels login.
     */
    @GetMapping(path = "/cancel", produces = MediaType.TEXT_HTML_VALUE)
    public String cancel(HttpSession session) {
        String ref = (String) session.getAttribute(SESSION_ATTRIBUTE_REF);
        PushedAuthorizationRequest pushedAuthorizationRequest = openIDConnectServer.getSDKConfiguration().getCache().getAuthorizationRequest(ref);
        AuthorizationResponse errorResponse = openIDConnectServer.errorResponse(pushedAuthorizationRequest, "access_denied", "User cancelled authentication with EU Digital Identity Wallet");
        RedirectedResponse response = (RedirectedResponse) openIDConnectServer.createClientResponse(errorResponse);
        return "redirect:" + response.toQueryRedirectUri();
    }

    /**
     * Create a QR code for cross-device login request.
     */
    @GetMapping(path = "/login/qrcode/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<BufferedImage> qrCode(@PathVariable("id") String id) throws Exception {
        com.nimbusds.oauth2.sdk.AuthorizationRequest crossDeviceRequest = openID4VPService.createAuthorizationRequest(OpenID4VPFlow.cross_device, id);
        return ResponseEntity.ok(createQRCodeImage(crossDeviceRequest.toURI().toString(), 200, 200, "PNG"));
    }

    private BufferedImage createQRCodeImage(String text, int width, int height, String format) throws Exception {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix =
                barcodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

}
