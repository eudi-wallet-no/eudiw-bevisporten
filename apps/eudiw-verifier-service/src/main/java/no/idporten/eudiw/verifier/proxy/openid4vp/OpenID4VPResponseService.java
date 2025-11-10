package no.idporten.eudiw.verifier.proxy.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.dataelement.EncodedCBORElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.MapKey;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import no.idporten.eudiw.verifier.proxy.VerificationException;
import no.idporten.eudiw.verifier.proxy.api.openid4vp.EncryptedAuthorizationResponse;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.VerifiedCredentials;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenID4VPResponseService {

    private final KeystoreManager keystoreManager;
    private final VerificationTransactionService verificationTransactionService;

    public OpenID4VPResponseService(KeystoreManager keystoreManager, VerificationTransactionService verificationTransactionService) {
        this.keystoreManager = keystoreManager;
        this.verificationTransactionService = verificationTransactionService;
    }

    public void receiveResponse(String verifierTransactionId, EncryptedAuthorizationResponse encryptedAuthorizationResponse) throws Exception {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction id");
        }
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(encryptedAuthorizationResponse.getResponse());
        String nonce = (String) claimsFromJwePayload.get("nonce");
        String state = (String) claimsFromJwePayload.get("state");
        final String vpToken = extractVpToken(verifierTransactionId, claimsFromJwePayload);
        final Map<String, Object> claims;
        if ("dc+sd-jwt".equals(verificationTransaction.getCredentialConfiguration().getFormat())) {
            claims = retrieveClaimsFromSDJwtCredential(vpToken);
        } else {
            claims = retrieveClaimsFromMDocCredential(vpToken);
        }
        VerifiedCredentials verifiedCredentials = new VerifiedCredentials(vpToken, claims);
        verificationTransactionService.addVerifiedCredentials(verifierTransactionId, verifiedCredentials);
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
        return JWTClaimsSet.parse(unverifiedSDJwt.getFullPayload().toString()).toJSONObject();
    }

    protected Map<String, Object> retrieveClaimsFromMDocCredential(String vpToken) throws Exception {
        DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
        Map<String, Object> claims = new HashMap<>();
        for (MDoc mDoc : deviceResponse.getDocuments()) {
            mDoc.getMSO(); // MSO (Mobile Security Object) verification is not performed here because the issuer's public key or certificate is not available in this context.
            // Proper MSO verification is critical for mdoc validation and should be implemented as soon as the issuer's public key can be obtained.
            // Failing to verify the MSO means the authenticity and integrity of the credential cannot be guaranteed.
            // TODO: Implement MSO verification using the issuer's public key or certificate when it becomes available.
            mDoc.verifyDocType();
            mDoc.verifyIssuerSignedItems();
            mDoc.verifyValidity();
            IssuerSigned issuerSigned = mDoc.getIssuerSigned();
            for (String namespace : issuerSigned.getNameSpaces().keySet()) {
                List<EncodedCBORElement> elements = issuerSigned.getNameSpaces().get(namespace);
                for (EncodedCBORElement element : elements) {
                    Map<MapKey, DataElement> elementMap = ((MapElement) element.decode()).getValue();
                    String elementIdentifier = null;
                    String elementValue = null;
                    for (MapKey mapKey : elementMap.keySet()) {
                        if (mapKey.getStr().equals("elementIdentifier")) {
                            elementIdentifier = String.valueOf(elementMap.get(mapKey).getInternalValue());
                        }
                        if (mapKey.getStr().equals("elementValue")) {
                            elementValue = String.valueOf(elementMap.get(mapKey).getInternalValue());
                        }
                    }
                    claims.put(elementIdentifier, elementValue);
                }
            }
        }
        return claims;
    }

}
