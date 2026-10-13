package no.idporten.eudiw.verifier.openid4vp;


import com.nimbusds.jose.util.X509CertUtils;
import id.walt.mdoc.COSECryptoProviderKeyInfo;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.doc.MDocVerificationParams;
import id.walt.mdoc.doc.VerificationType;
import id.walt.mdoc.issuersigned.IssuerSigned;
import id.walt.mdoc.mdocauth.DeviceAuthentication;
import id.walt.mdoc.mso.DeviceKeyInfo;
import id.walt.mdoc.mso.MSO;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cose.java.AlgorithmID;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MDocService  {


    private static final Logger log = LogManager.getLogger(MDocService.class);

    public MDocService() {
    }

    public DeviceResponse deviceResponseFromVpToken(String vpToken) {
        DeviceResponse deviceResponse;
        try {
            deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("must not be empty")) {
                throw new VerificationException("invalid_request", "No mdoc documents in vp_token", e);
            }
            throw new VerificationException("invalid_request", "Failed to parse mdoc vp_token", e);
        }
        if (deviceResponse.getDocuments().isEmpty()) {
            throw new VerificationException("invalid_request", "No mdoc documents in vp_token");
        }
        return deviceResponse;
    }

    /**
     * According to HAIP §5.3.1, only one document per DeviceResponse is supported.
     * @param deviceResponse this contains the single MDoc.
     * @return
     */
    public MDoc mDocFromDeviceResponse(DeviceResponse deviceResponse) {
        if(deviceResponse.getDocuments().size() > 1) {
            throw new VerificationException("invalid_request", "Multiple mdoc documents in vp_token, only one is supported");
        } else {
            return deviceResponse.getDocuments().getFirst();
        }
    }

    public Map<String, Object> claimsFromMDoc(MDoc mDoc) {
        Map<String, Object> claims = new HashMap<>();
        mDocClaims(mDoc.getIssuerSigned(), claims);
        return claims;
    }

    protected ValidationStatus verifyMDoc (id.walt.mdoc.doc.MDoc mDoc, VerificationTransaction verificationTransaction) {

        // First, check issuer signature

        // Second, check device signature:
        Boolean verifyDeviceSignature = verifyHolderBinding(mDoc, verificationTransaction);

        // Third, check validity period:
        boolean verifyValidity = mDoc.verifyValidity();

        // Fourth, check nonce:
        boolean verifyNonce = verifyNonce(mDoc, verificationTransaction);

        if (verifyDeviceSignature && verifyValidity && verifyNonce) {
            return ValidationStatus.VALID;
        } else {
            return ValidationStatus.INVALID;
        }

       // mDoc.getMSO(); // MSO (Mobile Security Object) verification is not performed here because the issuer's public key or certificate is not available in this context.
        // Proper MSO verification is critical for mdoc validation and should be implemented as soon as the issuer's public key can be obtained.
        // Failing to verify the MSO means the authenticity and integrity of the credential cannot be guaranteed.
        // TODO: Implement MSO verification using the issuer's public key or certificate when it becomes available.

    }

    public boolean verifyHolderBinding(MDoc mdoc, VerificationTransaction verificationTransaction) {


        PublicKey devicePublicKey = devicePublicKeyFromMso(mdoc);

        SimpleCOSECryptoProvider cryptoProvider = new SimpleCOSECryptoProvider(List.of(
                new COSECryptoProviderKeyInfo("123", AlgorithmID.ECDSA_256, devicePublicKey, null, List.of(extractCertificateFromMdoc(mdoc)), null)
        ));


        // Device authentication is the signed proof

        DeviceAuthentication deviceAuthentication = new DeviceAuthentication(mdoc.getDeviceSigned().getDeviceAuth().getDeviceSignature().getData());

        if (deviceAuthentication == null) {
            return false;
        }

        challengeMatches(deviceAuthentication, verificationTransaction.getNonce(), verificationTransaction.getAudience());
        try {
            return mdoc.verifyDeviceSignature(deviceAuthentication, cryptoProvider, null);
        } catch (Exception e) {
            return false;
        }
    }

    protected PublicKey devicePublicKeyFromMso(MDoc mdoc) {
        DeviceKeyInfo deviceKeyInfo = mdoc.getMSO().getDeviceKeyInfo();

        // This is a CBOR map of DataElement values, not a plain Java map
        MapElement deviceKeyElement = (MapElement) deviceKeyInfo.getDeviceKey();
        Map<MapKey, id.walt.mdoc.dataelement.DataElement> keyMap = deviceKeyElement.getValue();

        // COSE EC key:
        // 1 = kty, -1 = crv, -2 = x, -3 = y
        NumberElement kty = (NumberElement) keyMap.get(new MapKey(1));
        NumberElement crv = (NumberElement) keyMap.get(new MapKey(-1));
        ByteStringElement xElem = (ByteStringElement) keyMap.get(new MapKey(-2));
        ByteStringElement yElem = (ByteStringElement) keyMap.get(new MapKey(-3));

        if (kty == null || crv == null || xElem == null || yElem == null) {
            throw new IllegalArgumentException("Invalid COSE EC key in deviceKeyInfo");
        }

        // kty = 2 => EC2
        int ktyValue = kty.getValue().intValue();

        // crv = 1 => P-256
        int crvValue = crv.getValue().intValue();

        if (ktyValue != 2) {
            throw new IllegalArgumentException("Unsupported COSE key type: " + ktyValue);
        }

        if (crvValue != 1) {
            throw new IllegalArgumentException("Unsupported EC curve: " + crvValue);
        }

        byte[] xBytes = xElem.getValue();
        byte[] yBytes = yElem.getValue();

        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);

        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));

            ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
            ECPoint ecPoint = new ECPoint(x, y);

            ECPublicKeySpec spec = new ECPublicKeySpec(ecPoint, ecParameters);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to build EC public key from device key", e);
        }
    }


    protected boolean verifyNonce (MDoc mdoc, VerificationTransaction verificationTransaction) {
        return false;
    }

    private boolean challengeMatches(DeviceAuthentication deviceAuth, String expectedNonce, String expectedAudience) {
        // 1) Decode the signed COSE payload from the deviceAuth object
        DataElement signedPayload = decodeSignedPayload(deviceAuth);
        Object internalValue = signedPayload.getInternalValue();
        log.info("Signed payload internal value: {}", internalValue);

        if (signedPayload == null) {
            return false;
        }

        // 2) Extract the signed challenge values
        // Abstract names vary by implementation, but conceptually:
//        String signedNonce = extractString(signedPayload, "nonce");
//        String signedAudience = extractString(signedPayload, "aud");
return false;


        // 3) Compare them to expected values
//        return expectedNonce.equals(signedNonce)
//                && expectedAudience.equals(signedAudience);
    }


    private DataElement decodeSignedPayload(DeviceAuthentication deviceAuth) {
        try {
            byte[] payload = null;
            log.info("DeviceAuth Data: {}", deviceAuth.getData().getFirst());

            // Example: if your library exposes the COSE payload bytes:
             payload = deviceAuth.toCBOR();
            // or:
            // payload = deviceAuth.getDeviceMac().getPayload();

            if (payload == null) {
                return null;
            }

            // Decide how your library decodes CBOR.
            // For a generic map:


            return null; // replace with real CBOR decode
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * mdoc paths consist of a namespace and an element identifier. The claims are returned as a map of namespace
     * to a map of element identifier to value.
     **/

    protected Map<String, Object> mDocClaims (IssuerSigned issuerSigned, Map < String, Object > claims){
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
            case dateTime -> ((DateTimeElement) dataElement).getValue().toString();
            case fullDate -> ((FullDateElement) dataElement).getValue().toString();
            case nil -> null;
            case map -> ((MapElement) dataElement).getValue().entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> extractValue(e.getValue())));
            case list -> ((ListElement) dataElement).getValue()
                    .stream()
                    .map(this::extractValue)
                    .toList();
            case byteString -> new String(Base64.getEncoder().encode(((ByteStringElement) dataElement).getValue()));
            default -> String.valueOf(dataElement.getInternalValue());
        };

    }

    protected X509Certificate extractCertificateFromMdoc (MDoc mDoc){
        IssuerSigned issuerSigned = mDoc.getIssuerSigned();
        if (issuerSigned.getIssuerAuth() == null) {
            throw new VerificationException("invalid_request", "issuerAuth is missing in mdoc");
        }
        var issuerAuth = issuerSigned.getIssuerAuth();
        List<byte[]> x5chain = issuerAuth.getX5Chain();
        if (x5chain == null || x5chain.isEmpty()) {
            throw new VerificationException("invalid_request", "x5chain is missing in issuerAuth of mdoc");
        }
        byte[] leafDer = x5chain.getFirst();
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(leafDer));
            return cert;
        } catch (CertificateException e) {
            throw new VerificationException("invalid_request", "unable to extract certificate from issuerAuth x5chain mdoc", e);
        }
    }


    protected StatuslistEntry extractStatuslistUriAndIdx(MDoc mDoc) {
        if(!Objects.isNull(mDoc.getMSO()) && !Objects.isNull(mDoc.getMSO().getStatus()) && !Objects.isNull(mDoc.getMSO().getStatus().getStatusList())) {
            return new StatuslistEntry(mDoc.getMSO().getStatus().getStatusList().toJSON().get("idx").toString(), URI.create(mDoc.getMSO().getStatus().getStatusList().getUri()));
        }
        return null;
    }

    public @NonNull String getValidationDetail(ValidationStatus status) {
        switch (status) {
            case INCONCLUSIVE:
                return "mdoc: validering feila";
            case VALID:
                return "mdoc: mdoc er gyldig";
            case INVALID:
                return "mdoc: mdoc er ugyldig";
            default:
                return "mdoc: ukjent status";
        }
    }
}
