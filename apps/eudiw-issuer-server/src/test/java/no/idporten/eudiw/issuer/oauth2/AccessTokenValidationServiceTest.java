package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @DisplayName("then bearer and dpop authentication schemes can be used")
    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "DPoP"})
    void testValidateAccessTokenFromAuthorizationHeader(String authenticationScheme) throws Exception {
        AuthorizationServer authorizationServer = new AuthorizationServer();
        authorizationServer.setIssuer(URI.create("https://auth.eidas2sandkasse.dev"));
        AccessTokenValidator accessTokenValidator = mock(AccessTokenValidator.class);
        when(accessTokenValidator.validate(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);
        authorizationServer.setAccessTokenValidator(accessTokenValidator);
        final String signedJWTForTestPurposes = "eyJraWQiOiJkejFvTVJIcjlNZU9BNVJKcmFpNXotVFItTVl0ZjMzUGJoNkhrWkxkaWhrIiwidHlwIjoiYXQrSldUIiwiYWxnIjoiRVMyNTYifQ.eyJhdWQiOiJodHRwczovL3V0c3RlZGVyLmVpZGFzMnNhbmRrYXNzZS5kZXYiLCJzdWIiOiI1NzkyNjcwMDk0NiIsInhpZCI6ImV5SnJhV1FpT2lKcFpIQnZjblJsYmw5MFpYTjBYMk5sY25SZlpYaHdYekl3TWpjaUxDSmhiR2NpT2lKU1V6STFOaUo5LmV5SnpkV0lpT2lKSlpXWktWMUJuVGtGb2FVTlJRVkIxY0RWV1UzZHpZVXhrTkhCSFgybGFVMlo0UVRKV05tVXdWVjlwWmkxVE0zcEJhMmRqZUdaS1kxSkRibDlyYjJGQ2RIVmFZM2RDU1ZoV09FSkljalU1WTBoUVRXc2lMQ0poYlhJaU9sc2lWR1Z6ZEVsRUlsMHNJbWx6Y3lJNkltaDBkSEJ6T2k4dmRHVnpkQzVwWkhCdmNuUmxiaTV1YnlJc0luQnBaQ0k2SWpVM09USTJOekF3T1RRMklpd2liRzlqWVd4bElqb2libUlpTENKdWIyNWpaU0k2SWxBMVNtMUNSbTh5VW1kNVExZHRhbmhVTVZoMlprbGlZWGxhUzJObVdreExhRGRGVkdORmFIY3lTRlVpTENKaGRXUWlPaUpsZFdScGQxOWhkWFJvWDNCeWIzaDVJaXdpWVdOeUlqb2lhV1J3YjNKMFpXNHRiRzloTFhOMVluTjBZVzUwYVdGc0lpd2lZWFYwYUY5MGFXMWxJam94TnpZME16TXdNRE0yTENKbGVIQWlPakUzTmpRek16QXhOVFlzSW1saGRDSTZNVGMyTkRNek1EQXpOaXdpYW5ScElqb2llRFZJWTBGWldEaHVZa1VpZlEuVkpFdUFOc2dKVFlsZ0FGRDZFZm5RYy1GVW5GdGtZam5XVzZLQlRhM3RQTDZPdm14cUpFUlVidGFjVEVxMDRaLUhNenBvSUVYcnRmcGdGa1NWQUJyMmF3YUxHMldZUWlZcnppOEZfVW1QUWoyb1duNmJ5VnZQUXhNbTQ0a1Q1WktaT0ljNmlDOVpFbUUtQlJGM2dPYkZLLTYyc0Z6SFNzOWQxYW1YNkREM20zWlFrNmN0N1pJM1ItLXRlSndpeTF4cnFpV2VhdjVPTlp2TXZqdTExYlpvbk1MMEFBZW9OTDBYWE5uWlhYd2tqVno1X09nZTYwV2RRSnV0RTd2V25VeWZ1T3YxLTdESWxib3NLTGtQWlZmeW9PVFFESkJENXRwa0l0NXdueWdmcDEtZHdlcjJxel9tVDNUS3pKSGNxd3JLTnM4T29KbWw1emZIZjNaSVhvd1RXc1F6RUhsbUxPSE5rS1VLTjdVbXB6bE9yQU9UX2tsVXd6Rm9jYzZ2dG5zeVlQN0h0OEJKcGpqU1M1b2hMTFpkcDdZX3NaaUZ2bzVyb3diUmdsZXNlS3g4NmNRX0lRWHZQZ3VDOWFvRWZBQVNSZS1tVkVWdXBmY1g3YWlZaGVvRHRHSG8xYWFzSnM2WTRWcFJJR3N6NmxSSnFQWFBGZ3RhTmZRSkYzY3lYaUMiLCJzY29wZSI6ImV1ZGl3Om5vOnBpZCIsImlzcyI6Imh0dHBzOi8vYXV0aC5laWRhczJzYW5ka2Fzc2UuZGV2IiwieGF0IjoiZXlKcmFXUWlPaUpwWkhCdmNuUmxibDkwWlhOMFgyTmxjblJmWlhod1h6SXdNamNpTENKaGJHY2lPaUpTVXpJMU5pSjkuZXlKemRXSWlPaUkxTnpreU5qY3dNRGswTmlJc0ltRmpjaUk2SW1sa2NHOXlkR1Z1TFd4dllTMXpkV0p6ZEdGdWRHbGhiQ0lzSW5OamIzQmxJam9pYjNCbGJtbGtJSEJ5YjJacGJHVWlMQ0pwYzNNaU9pSm9kSFJ3Y3pvdkwzUmxjM1F1YVdSd2IzSjBaVzR1Ym04aUxDSmpiR2xsYm5SZllXMXlJam9pY0hKcGRtRjBaVjlyWlhsZmFuZDBJaXdpY0dsa0lqb2lOVGM1TWpZM01EQTVORFlpTENKbGVIQWlPakUzTmpRek16QXhOVFlzSW1saGRDSTZNVGMyTkRNek1EQXpOaXdpYW5ScElqb2llR2N3Um1GVGFtSnVOVTBpTENKamJHbGxiblJmYVdRaU9pSmxkV1JwZDE5aGRYUm9YM0J5YjNoNUlpd2lZMjl1YzNWdFpYSWlPbnNpWVhWMGFHOXlhWFI1SWpvaWFYTnZOalV5TXkxaFkzUnZjbWxrTFhWd2FYTWlMQ0pKUkNJNklqQXhPVEk2T1RreE9ESTFPREkzSW4xOS5VQWpUcXM3bm5QbTluek5ZMktDVGJFUWlZeXhoTGxzUEFaaWVRV05QY1JWMXJtZEQxMzdfd3c2VmQ0Q2JaQm1oUElPaVlUXzdtQ2Zmbzc4V09waXUwaFZNS3VTRk1qYmVNd1cxQno2TXBOX2p1Zm1OUE9EaVVIWTE4QmlVRktTWm1GV2tVdUMweEtRblRLLXZsX2l1eEt3U1Rjb01pQkVpRXB1ZWZwVEM2cU5RdEF0OGZQbTAzMVlNbHRFNWRQSU00Y2NxLThjNWNZS0trY2tmMUotLW9Eb1Fpd2VuNUFhSjhHN01HRUt3emFRQUdwWkZiVUNzWnVMeG1NWEpTWDBEZ0Z1S1JXM19INVd1b3hqMjIyUlZqUklhcnlaSVJwSmZaclVlRTFaaGhMS3o1UHRuQlJuRmhRaDZqOWxVUDRqRUdyODNobC11WG5vY0ZTRUJWS0Q3NjBKd05DQjNtRHJQQ3d1Wjg2Sm9qV244NU53MjVOSE8zZUJsR01KX1RJN210VzkxajQ5VDQySkhyeVFEWGtENHNGTzRyMmdKQXVyWTVsNkZRUlVSVXBLRHRHR1MxTURzX0pPU1VhbHAzMDF5dVdOQkMxbElIcDZZMm5kako1V0RMUmM0dG0xWFhJZHp6SHBfR25lQUFHS3g4NHgzTXoySG9lc0twRXdBbW5JaiIsImV4cCI6MTc2NDMzMDE1NiwiaWF0IjoxNzY0MzMwMDM2LCJqdGkiOiJ2QTVTWlFqblFZc3N3T2xGTkpiTUxnUkFadG1NakJRSVdRNnc0ci1GUENBIiwiY2xpZW50X2lkIjoiYzNjZTdhNmMtMmJiYi00YWJlLTkwOWMtNDFiYzk0NjNkM2M1In0.signature";
        final JWT validatedAccessToken = accessTokenValidationService.validateAccessTokenForCredentialConfiguration("%s %s".formatted(authenticationScheme, signedJWTForTestPurposes), List.of(authorizationServer));
        assertNotNull(validatedAccessToken);
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
