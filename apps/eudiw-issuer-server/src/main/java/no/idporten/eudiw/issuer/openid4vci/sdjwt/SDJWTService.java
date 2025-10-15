package no.idporten.eudiw.issuer.openid4vci.sdjwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import id.walt.sdjwt.DecoyMode;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SDPayload;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;

import java.security.cert.Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SDJWTService {

    private final KeystoreManager keystoreManager;
    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    public SDJWTService(KeystoreManager keystoreManager, CredentialIssuerServerProperties credentialIssuerServerProperties) {
        this.keystoreManager = keystoreManager;
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
    }

    public Credential issueCredential(JWK jwk, CredentialConfigurationProperties credentialConfigurationProperties, List<Claim> claims) throws Exception {
        return Credential.builder().credential(encode(createSDJwt(jwk, credentialConfigurationProperties, claims))).build();
    }

    protected String encode(SDJwt sdJwt) {
        return sdJwt.getJwt() + sdJwt.getDisclosures().stream().collect(Collectors.joining("~", "~", "~"));
    }

    protected SDJwt createSDJwt(JWK jwk, CredentialConfigurationProperties credentialConfigurationProperties, List<Claim> claims) throws Exception {
        KeyProvider keyProvider = keystoreManager.getKeyProvider(credentialConfigurationProperties.getKeyStoreName());
        JWSSigner jwsSigner = new ECDSASigner((ECPrivateKey) keyProvider.privateKey());

        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(JWSAlgorithm.ES256, jwsSigner, null);

        Date now = new Date();
        JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
        claimsSetBuilder.issuer(credentialIssuerServerProperties.getCredentialIssuer().toString());
        claimsSetBuilder.issueTime(now);
        claimsSetBuilder.notBeforeTime(now);
        claimsSetBuilder.claim("vct", credentialConfigurationProperties.getCredentialType());
        claimsSetBuilder.claim("_sd_alg", "sha-256");
        for(Claim claim : claims) {
            claimsSetBuilder.claim(claim.getPath().getLast(), claim.getValue().value());
        }
        JWTClaimsSet claimsSet = claimsSetBuilder.build();
        JWTClaimsSet.Builder undisclosedClaimsSetBuilder = new JWTClaimsSet.Builder(claimsSet);
        for(Claim claim : claims) {
            undisclosedClaimsSetBuilder.claim(claim.getPath().getLast(), null);
        }
        JWTClaimsSet undisclosedClaimsSet = undisclosedClaimsSetBuilder.build();
        SDPayload sdPayload = SDPayload.Companion.createSDPayload(claimsSet, undisclosedClaimsSet, DecoyMode.NONE, 42);
        SDJwt sdJwt = SDJwt.Companion.sign(
                sdPayload,
                cryptoProvider,
                toECJwk(keyProvider).getKeyID(),
                credentialConfigurationProperties.getFormat().formatIdentifier(),
                Collections.emptyMap());
        return sdJwt;
    }

    protected JWK toECJwk(KeyProvider keyProvider) throws Exception {
        List<Base64> encodedCertificates = new ArrayList<>();
        for (Certificate c : keyProvider.certificateChain()) {
            encodedCertificates.add(Base64.encode(c.getEncoded()));
        }
        ECPublicKey ecPublicKey = (ECPublicKey) keyProvider.publicKey();

        return new ECKey.Builder(Curve.P_256, ecPublicKey)
                .keyUse(KeyUse.SIGNATURE)
                .keyIDFromThumbprint()
                .x509CertChain(encodedCertificates)
                .privateKey(keyProvider.privateKey())
                .build();
    }

}
