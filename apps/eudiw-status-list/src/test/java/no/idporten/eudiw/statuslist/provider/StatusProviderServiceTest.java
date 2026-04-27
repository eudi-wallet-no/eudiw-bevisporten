package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.statuslist.exceptions.StatusListSigningException;
import no.idporten.eudiw.statuslist.service.StatusListService;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Map;

import static no.idporten.eudiw.statuslist.TestData.getKeystoreManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class StatusProviderServiceTest {

    @Spy
    private final StatusProviderProperties statusProviderProperties = new StatusProviderProperties(
            URI.create("https://junit.eidas2sandkasse.dev"),
            Duration.ofHours(1),
            Duration.ofMinutes(47)
    );

    @Mock
    KeyProvider mockKeyProvider;

    @Mock
    Certificate mockCertificate;

    @Mock
    StatusListService mockStatusListService;

    @Spy
    KeystoreManager keystoreManager = getKeystoreManager();

    @InjectMocks
    private StatusProviderService statusProviderService;

    @Test
    @DisplayName("Should generate a valid Status List Token as JWT")
    void getValidStatusListTest() throws Exception {
        when(mockStatusListService.getJsonStatuslist("1")).thenReturn("eNrbuRgAAhcBXQ");
        String testId = "1";

        JWT jwt = statusProviderService.getStatusList(testId);

        assertInstanceOf(SignedJWT.class, jwt);
        SignedJWT signedJWT = (SignedJWT) jwt;
        assertEquals("SIGNED", signedJWT.getState().toString());

        assertNotNull(signedJWT.getSignature());

        Certificate certificate =  keystoreManager.getKeyProvider("status-provider").certificate();
        RSAPublicKey publicKey = (RSAPublicKey)certificate.getPublicKey();
        JWSVerifier verifier = new RSASSAVerifier(publicKey);
        assertTrue(signedJWT.verify(verifier));

        JWSHeader header = signedJWT.getHeader();
        assertEquals("statuslist+jwt", header.getType().toString());
        assertEquals("RS256", header.getAlgorithm().getName());

        // x5c test. Remove when switching to JWKS
        assertEquals(Base64.encode(certificate.getEncoded()), header.getX509CertChain().getFirst());

        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        assertEquals("https://junit.eidas2sandkasse.dev/lists/1", claims.getSubject());
        assertEquals(47 * 60L, claims.getClaim("ttl"));

        Map<String, Object> statusList = claims.getJSONObjectClaim("status_list");
        assertEquals(1, statusList.get("bits"));
        assertEquals("eNrbuRgAAhcBXQ", statusList.get("lst"));


        Long iat = claims.getDateClaim("iat").toInstant().toEpochMilli() / 1000;
        Long exp = claims.getDateClaim("exp").toInstant().toEpochMilli() / 1000;
        assertEquals(60 * 60, exp - iat, 2);
    }

    @Test
    @DisplayName("Should throw StatusListSigningException if certificate.getEncoded throws CertificateEncodingException")
    void trowStatusListSigningExceptionTest1() throws Exception {
        when(mockStatusListService.getJsonStatuslist("1")).thenReturn("eNrbuRgAAhcBXQ");
        doReturn(mockKeyProvider).when(keystoreManager).getKeyProvider(any());
        doReturn(mockCertificate).when(mockKeyProvider).certificate();
        when(mockCertificate.getEncoded()).thenThrow(CertificateEncodingException.class);


        String testId = "1";

        assertThrows(StatusListSigningException.class, (
                () -> statusProviderService.getStatusList(testId)
        ));
    }
}
