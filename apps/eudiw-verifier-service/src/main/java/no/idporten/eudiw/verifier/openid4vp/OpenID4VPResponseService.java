package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONArrayUtils;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.api.openid4vp.EncryptedAuthorizationResponse;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.api.openid4vp.WalletCallback;
import no.idporten.eudiw.verifier.crypto.ECUtils;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OpenID4VPResponseService {

    private final VerificationTransactionService verificationTransactionService;

    public OpenID4VPResponseService(VerificationTransactionService verificationTransactionService) {
        this.verificationTransactionService = verificationTransactionService;
    }

    public WalletCallback receiveResponse(ClientApplication clientApplication, String verifierTransactionId, EncryptedAuthorizationResponse encryptedAuthorizationResponse) throws Exception {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(clientApplication, verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction id");
        }
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(encryptedAuthorizationResponse.getResponse(), verificationTransaction.getEncryptionKey());
        String nonce = (String) claimsFromJwePayload.get("nonce");
        String state = (String) claimsFromJwePayload.get("state");
        if (!Objects.equals(state, verificationTransaction.getState())) {
            throw new VerificationException("invalid_request", "Invalid state in authorization response");
        }
        List<DcqlCredentialQuery> requestedCredentials = getRequestedCredentialQueries(verificationTransaction);
        VpToken vpToken = extractVpToken(claimsFromJwePayload);
        Map<String, List<VerifiedCredential>> allCredentials = new LinkedHashMap<>();
        for (DcqlCredentialQuery credentialQuery : requestedCredentials) {
            String credentialId = credentialQuery.getId();
            List<VerifiablePresentation> verifiablePresentations = vpToken.getVerifiablePresentation(credentialId);
            if (verifiablePresentations.isEmpty()) {
                allCredentials.put(credentialId, List.of(new VerifiedCredential(Map.of())));
                continue;
            }
            String format = credentialQuery.getFormat();
            List<VerifiedCredential> parsedCredentials = new ArrayList<>();
            for (VerifiablePresentation verifiablePresentation : verifiablePresentations) {
                Map<String, Object> claims;
                if ("dc+sd-jwt".equals(format)) {
                    claims = retrieveClaimsFromSDJwtCredential(verifiablePresentation.value());
                } else if ("mso_mdoc".equals(format)) {
                    claims = retrieveClaimsFromMDocCredential(verifiablePresentation.value());
                } else {
                    throw new VerificationException("invalid_request", "Unsupported credential format: " + format);
                }
                parsedCredentials.add(new VerifiedCredential(claims));
            }
            allCredentials.put(credentialId, parsedCredentials);
        }
        VerifiedCredentials verifiedCredentials = new VerifiedCredentials(allCredentials);
        verificationTransactionService.addVerifiedCredentials(clientApplication, verifierTransactionId, verifiedCredentials);
        WalletCallback walletCallback = new WalletCallback();
        if ("same_device".equals(verificationTransaction.getFlow()) && verificationTransaction.getRedirectUri() != null) {
            walletCallback.setRedirectUri(verificationTransaction.getRedirectUri());
        }
        return walletCallback;
    }

    @SuppressWarnings("unchecked")
    private VpToken extractVpToken(Map<String, Object> claimsFromJwePayload) {
        Object vpTokenObject = claimsFromJwePayload.get("vp_token");
        if (vpTokenObject instanceof Map<?, ?> vpTokenMap) {
            Map<String, List<VerifiablePresentation>> verifiablePresentations = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : vpTokenMap.entrySet()) {
                if (!(entry.getKey() instanceof String credentialId)) {
                    throw new VerificationException("invalid_request", "Unsupported vp_token key type");
                }
                verifiablePresentations.put(credentialId, toVerifiablePresentations(entry.getValue(), credentialId));
            }
            return new VpToken(verifiablePresentations);
        }
        return new VpToken(Map.of());
    }

    private List<VerifiablePresentation> toVerifiablePresentations(Object vpTokenValue, String credentialId) {
        if (vpTokenValue instanceof List<?> presentations) {
            List<VerifiablePresentation> verifiablePresentations = new ArrayList<>();
            for (Object presentation : presentations) {
                if (!(presentation instanceof String)) {
                    throw new VerificationException("invalid_request", "Unsupported vp_token value type for credential id: " + credentialId);
                }
                verifiablePresentations.add(new VerifiablePresentation((String) presentation));
            }
            return verifiablePresentations;
        }
        if (vpTokenValue instanceof String presentation) {
            return List.of(new VerifiablePresentation(presentation));
        }
        throw new VerificationException("invalid_request", "Unsupported vp_token structure for credential id: " + credentialId);
    }

    private static List<DcqlCredentialQuery> getRequestedCredentialQueries(VerificationTransaction verificationTransaction) {
        if (verificationTransaction.getDcqlQuery() == null || verificationTransaction.getDcqlQuery().getCredentials() == null
                || verificationTransaction.getDcqlQuery().getCredentials().isEmpty()) {
            throw new VerificationException("invalid_request", "Missing credentials in dcql_query");
        }
        for (DcqlCredentialQuery credential : verificationTransaction.getDcqlQuery().getCredentials()) {
            if (credential.getId() == null || credential.getId().isBlank()) {
                throw new VerificationException("invalid_request", "Missing id in dcql_query credential");
            }
        }
        return verificationTransaction.getDcqlQuery().getCredentials();
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response, JWK encryptionKey) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(response);
        JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(jwe.getHeader(), encryptionKey.toECKey().toPrivateKey());
        jwe.decrypt(decrypter);
        return jwe.getPayload().toJSONObject();
    }

    protected Map<String, Object> retrieveClaimsFromSDJwtCredential(String vpToken) throws Exception{
        SDJwt unverifiedSDJwt = SDJwt.Companion.parse(vpToken);
        JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
        X509Certificate cert = X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
        JWSVerifier jwsVerifier = new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
        JWSAlgorithm jwsAlgorithm = ECUtils.jwsAlgorithmFromKey(cert.getPublicKey());
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(jwsAlgorithm, null, jwsVerifier);
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(cryptoProvider, null);
        if (!verificationResult.getVerified()) {
            throw new VerificationException("invalid_request", "Invalid vp_token signature or unverified disclosures");
        }
        Map<String, Object> claims = new HashMap<>();
        for (String disclosure : verificationResult.getSdJwt().getDisclosures()) {
            List<Object> parsedDisclosure = JSONArrayUtils.parse(new String(Base64.getUrlDecoder().decode(disclosure)));
            claims.put((String) parsedDisclosure.get(1), parsedDisclosure.get(2));
        }
        return claims;
    }

    /**
     * mdoc paths consist of a namespace and an element identifier. The claims are returned as a map of namespace
     * to a map of element identifier to value.
     * @param vpToken
     * @return extracted data
     */
    protected Map<String, Object> retrieveClaimsFromMDocCredential(String vpToken) {
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
                    Object elementValue = null;
                    for (MapKey mapKey : elementMap.keySet()) {
                        if (mapKey.getStr().equals("elementIdentifier")) {
                            elementIdentifier = String.valueOf(elementMap.get(mapKey).getInternalValue());
                        }
                        if (mapKey.getStr().equals("elementValue")) {
                            elementValue = extractValue(elementMap.get(mapKey));
                        }
                    }
                    Map<String, Object> nameSpaceMap = (Map<String, Object>) claims.computeIfAbsent(namespace, _ -> new HashMap<String, Object>());
                    nameSpaceMap.put(elementIdentifier, elementValue);
                }
            }
        }
        return claims;
    }
    
    protected Object extractValue(DataElement dataElement) {
        if (dataElement == null) {
            return null;
        }
        if (dataElement instanceof BooleanElement) {
            return ((BooleanElement) dataElement).getValue();
        }
        return switch (dataElement.getType()) {
            case number -> ((NumberElement) dataElement).getValue();
            case textString -> ((StringElement) dataElement).getValue();
            case dateTime ->  ((DateTimeElement) dataElement).getValue().toString();
            case fullDate ->  ((FullDateElement) dataElement).getValue().toString();
            case nil -> null;
            case map ->  ((MapElement) dataElement).getValue().entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> extractValue(e.getValue())));
            case list ->  ((ListElement) dataElement).getValue()
                    .stream()
                    .map(this::extractValue)
                    .toList();
            case byteString -> new String(Base64.getEncoder().encode(((ByteStringElement) dataElement).getValue()));
            default -> String.valueOf(dataElement.getInternalValue());
        };

    }
    

}
