package no.idporten.eudiw.issuer.api.openid4vci;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.openid4vci.protocol.NonceResponse;
import no.idporten.eudiw.issuer.openid4vci.nonce.NonceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RequiredArgsConstructor
@RestController
public class NonceEndpointController {

    private final NonceService nonceService;

    @PostMapping(path = Endpoints.NONCE_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NonceResponse> nonceEndpoint() {
        return ResponseEntity
                .ok(NonceResponse.builder()
                        .nonce(nonceService.generateNonce())
                        .build());
    }

}
