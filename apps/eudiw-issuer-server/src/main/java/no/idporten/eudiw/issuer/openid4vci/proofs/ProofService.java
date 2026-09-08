package no.idporten.eudiw.issuer.openid4vci.proofs;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.openid4vci.protocol.Proofs;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ProofService {

    private static final String JWT_PROOF_TYPE = "openid4vci-proof+jwt";
    private static final String KEY_ATTESTATION_TYPE = "key-attestation+jwt";
    private static final String KEY_ATTESTATION_HEADER = "key_attestation";
    private static final String ATTESTED_KEYS_CLAIM = "attested_keys";

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    /**
     * Validates proofs and returns binding keys.
     * @param credentialIssuerTenant tenant
     * @param proofs proofs
     * @return binding keys from validated proofs
     */
    public List<JWK> validateProofs(CredentialIssuerTenant credentialIssuerTenant, Proofs proofs) {
        List<JWK> bindingKeys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(proofs.getJwt())) {
            for (String jwtProof : proofs.getJwt()) {
                bindingKeys.addAll(validateJwtProof(credentialIssuerTenant, jwtProof));
            }
        }
        if (!CollectionUtils.isEmpty(proofs.getAttestation())) {
            for (String attestationProof : proofs.getAttestation()) {
                bindingKeys.addAll(validateAttestationProof(attestationProof));
            }
        }
        return bindingKeys;
    }

    private List<JWK> validateJwtProof(CredentialIssuerTenant credentialIssuerTenant, String jwtProof) {
        try {
            SignedJWT jwt = SignedJWT.parse(jwtProof);
            if (jwt.getHeader().getCustomParam(KEY_ATTESTATION_HEADER) != null) {
                return validateJwtProofWithKeyAttestation(credentialIssuerTenant, jwt);
            }
            return List.of(validateJwtProof(credentialIssuerTenant, jwt));
        } catch (ParseException e) {
            throw new InvalidProof("Invalid jwt proof.  Invalid JWT format.");
        } catch (JOSEException e) {
            throw new InvalidProof("Invalid jwt proof.  Invalid JWT signature.");
        } catch (BadJOSEException e) {
            throw new InvalidProof("Invalid jwt proof.  Invalid JWT claims.", e);
        }
    }

    private List<JWK> validateAttestationProof(String attestationProof) {
        try {
            return validateKeyAttestation(attestationProof);
        } catch (ParseException e) {
            throw new InvalidProof("Invalid key attestation.  Invalid JWT format.");
        } catch (JOSEException e) {
            throw new InvalidProof("Invalid key attestation.  Invalid JWT signature.");
        }
    }

    private JWK validateJwtProof(CredentialIssuerTenant credentialIssuerTenant, SignedJWT jwt) throws BadJOSEException, JOSEException {
        JWK jwk = jwt.getHeader().getJWK();
        if (jwk == null) {
            throw new InvalidProof("Invalid jwt proof.  Missing JWK header.");
        }
        validateBindingKey(jwk);
        JWK bindingKey = jwk.toPublicJWK();
        validateJwtSignatureAndClaims(credentialIssuerTenant, jwt, List.of(bindingKey));
        return bindingKey;
    }

    private List<JWK> validateJwtProofWithKeyAttestation(
            CredentialIssuerTenant credentialIssuerTenant,
            SignedJWT jwt) throws ParseException, BadJOSEException, JOSEException {
        Object keyAttestation = jwt.getHeader().getCustomParam(KEY_ATTESTATION_HEADER);
        if (!(keyAttestation instanceof String keyAttestationJwt)) {
            throw new InvalidProof("Invalid jwt proof.  Invalid key attestation header.");
        }

        List<JWK> attestedKeys = validateKeyAttestation(keyAttestationJwt);
        validateJwtSignatureAndClaims(credentialIssuerTenant, jwt, attestedKeys);
        return attestedKeys;
    }

    private void validateJwtSignatureAndClaims(
            CredentialIssuerTenant credentialIssuerTenant,
            SignedJWT jwt,
            List<JWK> bindingKeys) throws BadJOSEException, JOSEException {
        validateSigningAlgorithm(jwt.getHeader());
        List<PublicKey> publicKeys = toPublicKeys(bindingKeys);

        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector((_, _) -> publicKeys);
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                Collections.singleton(credentialIssuerTenant.getCredentialIssuer().toString()),
                null,
                Set.of("nonce", "iat"),
                null
        ));
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType(JWT_PROOF_TYPE)));
        jwtProcessor.process(jwt, (SecurityContext) null);
    }

    private List<JWK> validateKeyAttestation(String keyAttestationJwt) throws ParseException, JOSEException {
        SignedJWT keyAttestation = SignedJWT.parse(keyAttestationJwt);
        JWSHeader attestationHeader = keyAttestation.getHeader();
        validateSigningAlgorithm(attestationHeader);
        if (attestationHeader.getType() == null || !KEY_ATTESTATION_TYPE.equals(attestationHeader.getType().getType())) {
            throw new InvalidProof("Invalid key attestation.  Invalid JWT type.");
        }

        X509Certificate certificate = parseLeafCertificate(attestationHeader.getX509CertChain());
        JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(attestationHeader, certificate.getPublicKey());
        if (!keyAttestation.verify(verifier)) {
            throw new InvalidProof("Invalid key attestation.  Invalid JWT signature.");
        }

        List<?> attestedKeysClaim = keyAttestation.getJWTClaimsSet().getListClaim(ATTESTED_KEYS_CLAIM);
        if (attestedKeysClaim == null || attestedKeysClaim.isEmpty()) {
            throw new InvalidProof("Invalid key attestation.  Missing attested keys.");
        }

        List<JWK> attestedKeys = new ArrayList<>();
        for (Object attestedKey : attestedKeysClaim) {
            if (!(attestedKey instanceof Map<?, ?> attestedKeyMap)) {
                throw new InvalidProof("Invalid key attestation.  Invalid attested key.");
            }
            JWK jwk = JWK.parse(toStringKeyedMap(attestedKeyMap));
            validateBindingKey(jwk);
            attestedKeys.add(jwk.toPublicJWK());
        }
        return attestedKeys;
    }

    private Map<String, Object> toStringKeyedMap(Map<?, ?> map) {
        Map<String, Object> stringKeyedMap = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new InvalidProof("Invalid key attestation.  Invalid attested key.");
            }
            stringKeyedMap.put(key, entry.getValue());
        }
        return stringKeyedMap;
    }

    private X509Certificate parseLeafCertificate(List<Base64> certificateChain) {
        if (certificateChain == null || certificateChain.isEmpty()) {
            throw new InvalidProof("Invalid key attestation.  Missing x5c header.");
        }

        X509Certificate certificate = X509CertUtils.parse(certificateChain.getFirst().decode());
        if (certificate == null) {
            throw new InvalidProof("Invalid key attestation.  Invalid x5c certificate.");
        }
        return certificate;
    }

    private void validateSigningAlgorithm(JWSHeader jwsHeader) {
        if (!credentialIssuerServerProperties.getProofSigningAlgorithms().contains(jwsHeader.getAlgorithm().getName())) {
            throw new InvalidProof("Invalid jwt proof.  Invalid signing algorithm.");
        }
    }

    private void validateBindingKey(JWK jwk) {
        if (!KeyType.EC.equals(jwk.getKeyType())) {
            throw new InvalidProof("Invalid jwt proof.  Invalid key type.");
        }
    }

    private List<PublicKey> toPublicKeys(List<JWK> bindingKeys) throws JOSEException {
        List<PublicKey> publicKeys = new ArrayList<>();
        for (JWK bindingKey : bindingKeys) {
            publicKeys.add(((ECKey) bindingKey).toPublicKey());
        }
        return publicKeys;
    }

}
