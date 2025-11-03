package no.idporten.eudiw.issuer.openid4vci.sdjwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.JWK;
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

import java.security.interfaces.ECPrivateKey;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        claimsSetBuilder.expirationTime(Date.from(now.toInstant().plus(365, ChronoUnit.DAYS)));
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
                null,
                credentialConfigurationProperties.getFormat().formatIdentifier(),
                Map.of("x5c", List.of(Base64.getEncoder().encodeToString(keyProvider.certificate().getEncoded()))));
        return sdJwt;
    }

}
