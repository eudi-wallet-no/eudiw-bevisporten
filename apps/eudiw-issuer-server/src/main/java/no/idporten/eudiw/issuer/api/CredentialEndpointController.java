package no.idporten.eudiw.issuer.api;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.claimssource.CredentialIssuerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialResponse;
import no.idporten.eudiw.issuer.openid4vci.protocol.InvalidCredentialRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class CredentialEndpointController {

    private final CredentialIssuerService credentialIssuerService;

    @PostMapping(path = Endpoints.CREDENTIAL_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialResponse> credentialEndpoint(@RequestBody CredentialRequest credentialRequest) {
        credentialRequest.validate();
        List<Credential> credentials = credentialIssuerService.issueCredentials(credentialRequest);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(CredentialResponse.builder()
                        .credentials(credentials)
                        .build());
    }

    @ExceptionHandler(InvalidCredentialRequest.class)
    public ResponseEntity<ErrorResponse> invalidCredentialRequest(InvalidCredentialRequest invalidCredentialRequest) {
        return ResponseEntity.badRequest().body(new ErrorResponse(invalidCredentialRequest.getError(), invalidCredentialRequest.getErrorDescription()));
    }

}
