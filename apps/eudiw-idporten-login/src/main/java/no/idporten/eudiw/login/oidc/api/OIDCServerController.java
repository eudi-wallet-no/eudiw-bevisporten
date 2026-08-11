package no.idporten.eudiw.login.oidc.api;

import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.stream.Collectors;

import static no.idporten.sdk.oidcserver.OAuth2Exception.INVALID_REQUEST;
import static no.idporten.sdk.oidcserver.OAuth2Exception.SERVER_ERROR;

/**
 * The OIDC API for this application exposed the backend OAuth2/OIDC endpoints.
 */
@Controller
public class OIDCServerController {

    private static final Logger logger = LoggerFactory.getLogger(OIDCServerController.class);

    private final OpenIDConnectIntegration openIDConnectServer;

    public OIDCServerController(OpenIDConnectIntegration openIDConnectServer) {
        this.openIDConnectServer = openIDConnectServer;
    }

    @GetMapping("/")
    public String redirectIndexToMetadata() {
        return "redirect:/.well-known/openid-configuration";
    }

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenIDProviderMetadataResponse> openIDConnectProviderMetadata() {
        return ResponseEntity.ok(openIDConnectServer.getOpenIDProviderMetadata());
    }
    @GetMapping(value = {"/jwk", "/jwks", "/.well-known/jwks.json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> jwks() {
        return ResponseEntity.ok(openIDConnectServer.getPublicJWKSet().toString());
    }

    @PostMapping(value = "/par",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<PushedAuthorizationResponse> par(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters) {
        PushedAuthorizationResponse pushedAuthorizationResponse = openIDConnectServer.process(new PushedAuthorizationRequest(headers, parameters));
        return ResponseEntity.status(pushedAuthorizationResponse.getHttpStatusCode()).body(pushedAuthorizationResponse);
    }

    @PostMapping(value = "/token",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> token(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok(openIDConnectServer.process(new TokenRequest(headers, parameters)));
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<ErrorResponse> handleOAuth2Exception(OAuth2Exception e) {
        logger.warn(e.getMessage(), e);
        return ResponseEntity.status(e.getHttpStatusCode()).body(e.errorResponse());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {

        String bindingErrorMessages =
                e.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(","));
        return errorResponseEntity(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), bindingErrorMessages);
    }

    protected String errorMessageForHttpStatus(HttpStatusCode httpStatus) {
        if (httpStatus.is4xxClientError()) {
            return INVALID_REQUEST;
        }
        return SERVER_ERROR;
    }

    protected static ResponseEntity<ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, String error, String errorDescription) {
        return errorResponseEntity(httpStatus, ErrorResponse.builder().error(error).errorDescription(errorDescription).build());
    }

    protected static ResponseEntity<ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, ErrorResponse errorResponse) {
        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}
