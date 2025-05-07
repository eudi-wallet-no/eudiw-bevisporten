package no.idporten.eudiw.issuer.api;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.openid4vci.Credential;
import no.idporten.eudiw.issuer.openid4vci.CredentialRequest;
import no.idporten.eudiw.issuer.openid4vci.CredentialResponse;
import no.idporten.eudiw.issuer.openid4vci.InvalidCredentialRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CredentialEndpointController {

    @PostMapping(path = Endpoints.CREDENTIAL_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialResponse> credentialEndpoint(@RequestBody CredentialRequest credentialRequest) {
        credentialRequest.validate();
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(CredentialResponse.builder()
                        .credential(Credential.builder()
                                .credential("bar")
                                .build())
                        .build());
    }

    @ExceptionHandler(InvalidCredentialRequest.class)
    public ResponseEntity<ErrorResponse> invalidCredentialRequest(InvalidCredentialRequest invalidCredentialRequest) {
        return ResponseEntity.badRequest().body(new ErrorResponse(invalidCredentialRequest.getError(), invalidCredentialRequest.getErrorDescription()));
    }

}
