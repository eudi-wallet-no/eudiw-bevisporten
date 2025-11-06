package no.idporten.eudiw.verifier.proxy.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SDisclosure;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import kotlinx.serialization.json.JsonPrimitive;
import no.idporten.eudiw.verifier.proxy.api.openid4vp.EncryptedAuthorizationResponse;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OpenID4VPResponseService {

    private final KeystoreManager keystoreManager;
    private final VerificationTransactionService verificationTransactionService;

    public OpenID4VPResponseService(KeystoreManager keystoreManager, VerificationTransactionService verificationTransactionService) {
        this.keystoreManager = keystoreManager;
        this.verificationTransactionService = verificationTransactionService;
    }

    public void receiveResponse(String verifierTransactionId, EncryptedAuthorizationResponse encryptedAuthorizationResponse) throws Exception {
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(encryptedAuthorizationResponse.getResponse());
        // TODO
        String nonce = (String) claimsFromJwePayload.get("nonce");
        String state = (String) claimsFromJwePayload.get("state");
        final String vpToken = extractVpToken(verifierTransactionId, claimsFromJwePayload);
        // TODO format specific?
        final Map<String, Object> claims = retrieveClaimsFromSDJwtCredential(vpToken);
        verificationTransactionService.addVerifiedCredentials(verifierTransactionId, claims);
    }

    @SuppressWarnings("unchecked")
    private static String extractVpToken(String verifierTransactionId, Map<String, Object> claimsFromJwePayload) {
        final String vpToken;
        Map<String, Object> credentialsMap = (Map<String, Object>) claimsFromJwePayload.get("vp_token");
        Object vpTokenObject = credentialsMap.get(verifierTransactionId);
        vpToken = ((List<String>) vpTokenObject).getFirst();
        return vpToken;
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(response);
        JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(jwe.getHeader(), keystoreManager.getKeyProvider("access").privateKey());
        jwe.decrypt(decrypter);
        return jwe.getPayload().toJSONObject();
    }

    protected Map<String, Object> retrieveClaimsFromSDJwtCredential(String vpToken) throws Exception{
        SDJwt unverifiedSDJwt = SDJwt.Companion.parse(vpToken);
        JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
        X509Certificate cert = X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
        JWSVerifier jwsVerifier = new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(JWSAlgorithm.ES256, null, jwsVerifier);
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(cryptoProvider, null);
        SDJwt verifiedSDJwt = verificationResult.getSdJwt();
        SignedJWT signedJWT = SignedJWT.parse(verifiedSDJwt.getJwt());
        Map<String, Object> extractedClaims = verifiedSDJwt
                .getDisclosureObjects()
                .stream()
                .collect(Collectors.toMap(SDisclosure::getKey, sDisclosure -> List.of(getValue(sDisclosure))));

        return extractedClaims;
    }

    protected Object getValue(SDisclosure sDisclosure) {
        if (sDisclosure.getValue() instanceof JsonPrimitive) {
            return ((JsonPrimitive) sDisclosure.getValue()).getContent();
        }
        return sDisclosure.getValue().toString();
    }

}
