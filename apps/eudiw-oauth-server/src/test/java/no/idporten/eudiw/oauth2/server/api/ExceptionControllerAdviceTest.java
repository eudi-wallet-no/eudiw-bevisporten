package no.idporten.eudiw.oauth2.server.api;

import no.idporten.eudiw.oauth2.server.OAuth2Exception;
import no.idporten.eudiw.oauth2.server.UseAttestationChallengeOAuth2Exception;
import no.idporten.eudiw.oauth2.server.protocol.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExceptionControllerAdviceTest {

    @Test
    void testUseAttestationChallengeErrorContainsChallengeHeader() {
        ExceptionControllerAdvice exceptionControllerAdvice = new ExceptionControllerAdvice();
        UseAttestationChallengeOAuth2Exception exception = new UseAttestationChallengeOAuth2Exception("Invalid challenge", "next-challenge");

        ResponseEntity<ErrorResponse> response = exceptionControllerAdvice.handleUseAttestationChallengeException(exception);

        assertEquals(401, response.getStatusCode().value());
        assertEquals(
                "next-challenge",
                response.getHeaders().getFirst(UseAttestationChallengeOAuth2Exception.OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER)
        );
        assertNotNull(response.getBody());
        assertEquals(OAuth2Exception.USE_ATTESTATION_CHALLENGE, response.getBody().getError());
    }
}
