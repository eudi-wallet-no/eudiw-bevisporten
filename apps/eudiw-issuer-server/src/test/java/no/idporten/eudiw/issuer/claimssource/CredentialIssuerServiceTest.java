package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When issuing credentials")
@ExtendWith(MockitoExtension.class)
public class CredentialIssuerServiceTest {

    @Mock
    private CredentialIssuerServerProperties credentialIssuerServerProperties;

    @Mock
    private ClaimsSourceService claimsSourceService;

    @Mock
    private AccessTokenValidationService accessTokenValidationService;

    @InjectMocks
    private CredentialIssuerService credentialIssuerService;

    @DisplayName("then a valid credential request with access_token from valid authorization with valid scope will invoke the correct claims source")
    @Test
    void testInvokeClaimsSource() {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer("https://junit.idporten.no")
                .claim("scope", "openid profile foo:bar")
                .build();
        PlainJWT accessToken = new PlainJWT(jwtClaimsSet);
        CredentialRequest credentialRequest = CredentialRequest.builder().credentialConfigurationId("cid").build();
        CredentialConfigurationProperties credentialConfigurationProperties = CredentialConfigurationProperties.builder()
                .authorizationServer("https://junit.idporten.no")
                .scope("foo:bar")
                .doctype("foodoc")
                .build();
        ClaimsSource claimsSource = mock(ClaimsSource.class);
        when(claimsSource.retrieveClaims(eq(accessToken))).thenReturn(List.of(Claim.builder().path("n1").path("p1").value("v1").build()));
        when(credentialIssuerServerProperties.findCredentialConfiguration(eq("cid"))).thenReturn(credentialConfigurationProperties);
        when(claimsSourceService.findClaimsSource(eq("foodoc"))).thenReturn(claimsSource);
        List<Credential> credentials = credentialIssuerService.issueCredentials(credentialRequest, accessToken);
        assertAll(
                () -> assertEquals(1, credentials.size()),
                () -> assertTrue(credentials.getFirst().getCredential().contains("n1")),
                () -> assertTrue(credentials.getFirst().getCredential().contains("p1")),
                () -> assertTrue(credentials.getFirst().getCredential().contains("v1"))
        );
        verify(accessTokenValidationService).validateAccessTokenForCredentialConfiguration(eq(accessToken), eq("https://junit.idporten.no"), eq("foo:bar"));
    }

}
