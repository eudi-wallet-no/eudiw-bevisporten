package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When validating access tokens for secured requests")
@ActiveProfiles("junit")
@SpringBootTest
public class AccessTokenValidationServiceTest {

    @Autowired
    private AccessTokenValidationService accessTokenValidationService;

    @MockitoBean
    private AuditLogger auditLogger;

    @DisplayName("then an authorization header with bearer token is required")
    @Test
    void testInvalidAuthorizationHeader() throws Exception {
        IssuerServerException e = assertThrows(IssuerServerException.class, () -> accessTokenValidationService.validateAccessTokenForCredentialConfiguration((String) null, Collections.emptyList()));
        assertAll(
                () -> assertEquals("invalid_request", e.getError()),
                () -> assertTrue(e.getErrorDescription().contains("Missing authorization header")),
                () -> assertEquals(401, e.getHttpStatus().value())
        );
    }

    @DisplayName("then the access_token must contain a valid scope for the credential")
    @Test
    void testInvalidScope() {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer("https://junit.idporten.no")
                .claim("scope", "openid profile foo:bar foo")
                .build();
        PlainJWT accessToken = new PlainJWT(jwtClaimsSet);
        IssuerServerException e = assertThrows(IssuerServerException.class, () -> accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, "https://junit.idporten.no", "bar"));
        assertAll(
                () -> assertEquals("insufficient_scope", e.getError()),
                () -> assertTrue(e.getErrorDescription().contains("Invalid scope")),
                () -> assertEquals(403, e.getHttpStatus().value())
        );
    }

    @DisplayName("then the access_token must be issued by a valid authorization server for the credential")
    @Test
    void testInvalidAuthorizationServer() {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer("https://junit.idporten.no")
                .build();
        PlainJWT accessToken = new PlainJWT(jwtClaimsSet);
        IssuerServerException e = assertThrows(IssuerServerException.class, () -> accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, "https://unknown.junit.idporten.no", "bar"));
        assertAll(
                () -> assertEquals("invalid_token", e.getError()),
                () -> assertTrue(e.getErrorDescription().contains("Invalid authorization server")),
                () -> assertEquals(401, e.getHttpStatus().value())
        );
    }

}
