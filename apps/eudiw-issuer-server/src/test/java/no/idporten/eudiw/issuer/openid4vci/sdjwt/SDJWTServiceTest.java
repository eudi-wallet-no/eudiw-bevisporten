package no.idporten.eudiw.issuer.openid4vci.sdjwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimValue;
import no.idporten.eudiw.issuer.claimssource.domain.StringValue;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("junit")
@SpringBootTest
public class SDJWTServiceTest {

    @Autowired
    private SDJWTService sdjwtService;

    @BeforeAll
    static void addBouncyCastle() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @MockitoBean
    private AuditLogger auditLogger;

    private CredentialConfigurationProperties credentialConfigurationProperties(String docType, String keyStoreName) {
        CredentialConfigurationProperties credentialConfigurationProperties = new CredentialConfigurationProperties();
        credentialConfigurationProperties.setCredentialType(docType);
        credentialConfigurationProperties.setFormat(CredentialFormat.SD_JWT_VC);
        credentialConfigurationProperties.setKeyStoreName(keyStoreName);
        return credentialConfigurationProperties;
    }

    @Test
    void testIssueSDJwt() throws Exception {
        Claim stringClaim1 = buildClaim("foo", "string1", new StringValue("foobar"));
        Claim stringClaim2 = buildClaim("foo", "string2", new StringValue("foobar-foooooo"));
        CredentialConfigurationProperties credentialConfigurationProperties = credentialConfigurationProperties(
                "foo",
                "eaa-provider");
        SDJwt sdJwt = sdjwtService.createSDJwt(generateDeviceKey(), credentialConfigurationProperties, List.of(stringClaim1, stringClaim2));
        String encodedSDJwt = sdjwtService.encode(sdJwt);
        assertAll(
                () -> assertNotNull(sdJwt)
        );
        SDJwt unverifiedSDJwt = SDJwt.Companion.parse(encodedSDJwt);
        JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
        X509Certificate cert = X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
        JWSVerifier jwsVerifier = new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(JWSAlgorithm.ES256, null, jwsVerifier);
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(cryptoProvider, null);
        assertAll(
                () -> assertTrue(verificationResult.getVerified()),
                () -> assertTrue(verificationResult.getSignatureVerified()),
                () -> assertTrue(verificationResult.getDisclosuresVerified())
        );
        SDJwt verifiedSDJwt = verificationResult.getSdJwt();
        assertAll(
                () -> assertEquals(2, verifiedSDJwt.getDisclosures().size())
        );
    }

    private static Claim buildClaim(String path, String key, ClaimValue value) {
        return Claim.builder().path(path).path(key).value(value).build();
    }

    private static ECKey generateDeviceKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256).generate();
    }

}
