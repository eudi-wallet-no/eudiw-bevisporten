package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.statuslist.exceptions.StatusListSigningException;
import no.idporten.eudiw.statuslist.service.CompressedStatusList;
import no.idporten.eudiw.statuslist.service.StatusListService;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.time.Duration;
import java.util.Map;

import static no.idporten.eudiw.statuslist.TestData.getKeystoreManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


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

    @ParameterizedTest
    @ValueSource(strings = {"RSA", "EC"})
    @DisplayName("Should generate a valid Status List Token as JWT")
    void getValidStatusListTest(String keyType) throws Exception {
        KeystoreManager keystoreManager = getKeystoreManager(keyType);
        StatusProviderService statusProviderService = new StatusProviderService(statusProviderProperties, keystoreManager, mockStatusListService);
        CompressedStatusList compressedStatusList = new CompressedStatusList("eNrbuRgAAhcBXQ", 1);
        when(mockStatusListService.getJsonStatusList(1)).thenReturn(compressedStatusList);
        int testId = 1;

        JWT jwt = statusProviderService.getStatusList(testId);

        assertInstanceOf(SignedJWT.class, jwt);
        SignedJWT signedJWT = (SignedJWT) jwt;
        assertEquals("SIGNED", signedJWT.getState().toString());

        assertNotNull(signedJWT.getSignature());

        JWSHeader header = signedJWT.getHeader();
        Certificate certificate =  keystoreManager.getKeyProvider("status-provider").certificate();
        PublicKey publicKey = certificate.getPublicKey();
        JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey);
        assertTrue(signedJWT.verify(verifier));

        assertEquals("statuslist+jwt", header.getType().toString());
        assertEquals("RSA".equals(keyType) ? "RS256" : "ES256", header.getAlgorithm().getName());

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
        KeystoreManager keystoreManager = spy(getKeystoreManager("RSA"));
        StatusProviderService statusProviderService = new StatusProviderService(statusProviderProperties, keystoreManager, mockStatusListService);
        CompressedStatusList compressedStatusList = new CompressedStatusList("eNrbuRgAAhcBXQ", 1);
        when(mockStatusListService.getJsonStatusList(1)).thenReturn(compressedStatusList);
        doReturn(mockKeyProvider).when(keystoreManager).getKeyProvider(any());
        doReturn(mockCertificate).when(mockKeyProvider).certificate();
        when(mockCertificate.getEncoded()).thenThrow(CertificateEncodingException.class);


        int testId = 1;

        StatusListSigningException e = assertThrows(StatusListSigningException.class, (
                () -> statusProviderService.getStatusList(testId)
        ));

        assertEquals("Failed to encode certificate", e.getMessage());
        assertEquals("server_error", e.getErrorCode());
    }
}
