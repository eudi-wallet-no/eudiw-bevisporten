package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.openid4vci.protocol.Proof;
import no.idporten.eudiw.issuer.openid4vci.protocol.Proofs;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Collections;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ProofService {

    public static final String PROOF_TYPE_JWT = "jwt";

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    @Deprecated
    public void validateProof(CredentialIssuerTenant credentialIssuerTenant, Proof proof) {
            if (PROOF_TYPE_JWT.equals(proof.getProofType())) {
                validateJwtProof(credentialIssuerTenant, proof.getJwt());
            } else {
                throw new InvalidProof(null, "Unsupported proof type.");
        }
    }

    public void validateProofs(CredentialIssuerTenant credentialIssuerTenant, Proofs proofs) {
        for (String jwtProof : proofs.getJwt()) {
            validateJwtProof(credentialIssuerTenant, jwtProof);
        }
    }

    public void validateJwtProof(CredentialIssuerTenant credentialIssuerTenant, String jwtProof) {
        try {
            SignedJWT jwt = SignedJWT.parse(jwtProof);
            JWSHeader jwsHeader = jwt.getHeader();
            JWK jwk = jwsHeader.getJWK();
            if (jwk == null) {
                throw new InvalidProof("Invalid jwt proof.  Missing JWK header.");
            }
            if (!KeyType.EC.equals(jwk.getKeyType())) {
                throw new InvalidProof("Invalid jwt proof.  Invalid key type.");
            }
            if (! credentialIssuerServerProperties.getProofSigningAlgorithms().contains(jwsHeader.getAlgorithm().getName())) {
                throw new InvalidProof("Invalid jwt proof.  Invalid signing algorithm.");
            }
            ECKey ecKey = (ECKey) jwk;
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(new SingleKeyJWSKeySelector<>(JWSAlgorithm.ES256, ecKey.toPublicKey()));
            jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                    Collections.singleton(credentialIssuerTenant.getCredentialIssuer().toString()),
                    null,
                    Set.of("nonce", "iat"),
                    null
            ));
            jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("openid4vci-proof+jwt")));
            jwtProcessor.process(jwt, (SecurityContext) null);
        } catch (ParseException e) {
            throw new InvalidProof("Invalid jwt proof.  Invalid JWT format.");
        } catch (JOSEException e) {
            throw new InvalidProof("Invalid jwt proof.  Invalid JWT signature.");
        } catch (BadJOSEException e) {
            throw new InvalidProof("Invalid jwt proof.  Invalid JWT claims.", e);
        }
    }

}
