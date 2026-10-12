package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.util.X509CertUtils;
import id.walt.mdoc.COSECryptoProviderKeyInfo;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import org.cose.java.AlgorithmID;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MDocService {


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
     * According to HAIP §5.3.1, only one document per DeviceResponse is supported. See also HAIP 1.0.4
     * https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html#name-openid-for-verifiable-crede
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

    protected ValidationStatus verifyMDoc(id.walt.mdoc.doc.MDoc mDoc, VerificationTransaction verificationTransaction) {

        // Step one, verify the isuer signature
        X509Certificate issuerCertificate = extractCertificateFromMdoc(mDoc);
        Boolean issuerSignatureValid = verifyIssuerSignature(mDoc, issuerCertificate);


        mDoc.getMSO(); // MSO (Mobile Security Object) verification is not performed here because the issuer's public key or certificate is not available in this context.
        // Proper MSO verification is critical for mdoc validation and should be implemented as soon as the issuer's public key can be obtained.
        // Failing to verify the MSO means the authenticity and integrity of the credential cannot be guaranteed.
        // TODO: Implement MSO verification using the issuer's public key or certificate when it becomes available.
        boolean verifyDocType = mDoc.verifyDocType();
        boolean verifyIssuerSignedItems = mDoc.verifyIssuerSignedItems();
        boolean verifyValidity = mDoc.verifyValidity();
        if (verifyDocType && verifyIssuerSignedItems && verifyValidity && issuerSignatureValid != null && issuerSignatureValid) {
            return ValidationStatus.VALID;
        } else {
            return ValidationStatus.INVALID;
        }
    }

    protected Boolean verifyIssuerSignature(MDoc mdoc, X509Certificate issuerCertificate) {
        X509Certificate rootCa = X509CertUtils.parse("-----BEGIN CERTIFICATE-----MIICMzCCAdmgAwIBAgIIbLk59uoMYY4wCgYIKoZIzj0EAwIwfzEeMBwGA1UEYRMVTlRSTk8tTk9GT1IuOTkxODI1ODI3MQswCQYDVQQGEwJOTzEkMCIGA1UEChMbRElHSVRBTElTRVJJTkdTRElSRUtUT1JBVEVUMSowKAYDVQQDEyFlaWRhczJzYW5ka2Fzc2Ugcm9vdCBDQSAyIHN5c3Rlc3QwHhcNMjYwMzExMDYyODE2WhcNMzEwMzEwMDYyODE2WjB/MR4wHAYDVQRhExVOVFJOTy1OT0ZPUi45OTE4MjU4MjcxCzAJBgNVBAYTAk5PMSQwIgYDVQQKExtESUdJVEFMSVNFUklOR1NESVJFS1RPUkFURVQxKjAoBgNVBAMTIWVpZGFzMnNhbmRrYXNzZSByb290IENBIDIgc3lzdGVzdDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABEPMSVuEwHRIVXmHGxCeXStQRZLN19EDpgO8sPyqgckHAXL0UuE4uuV4mtoUmTfwlZyGo6d0/IrXq0sE/5V7fAWjPzA9MB0GA1UdDgQWBBQUeG7cOumLe28MkYC1zhwUuck/8jAOBgNVHQ8BAf8EBAMCAQYwDAYDVR0TBAUwAwEB/zAKBggqhkjOPQQDAgNIADBFAiEAqhJeWoE7mHLhMRvtYwO5XN96ttIp9f3OVHMaTb3dyv4CIGXjpOYK5+shq416BS47WGsq01TWAaU5fHvnVo/pz9c8-----END CERTIFICATE-----");
        COSECryptoProviderKeyInfo keyInfo = new COSECryptoProviderKeyInfo(issuerCertificate.getSubjectX500Principal().getName(), AlgorithmID.ECDSA_256, issuerCertificate.getPublicKey(), null, List.of(issuerCertificate), List.of(rootCa));
        SimpleCOSECryptoProvider cryptoProvider = new SimpleCOSECryptoProvider(List.of(keyInfo));
        return mdoc.verifySignature(cryptoProvider, issuerCertificate.getSubjectX500Principal().getName());
    }

    /**
     * mdoc paths consist of a namespace and an element identifier. The claims are returned as a map of namespace
     * to a map of element identifier to value.
     **/

    protected Map<String, Object> mDocClaims(IssuerSigned issuerSigned, Map<String, Object> claims) {
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

    protected X509Certificate extractCertificateFromMdoc(MDoc mDoc) {
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
        X509Certificate cert;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(leafDer));
        } catch (CertificateException e) {
            throw new VerificationException("invalid_request", "unable to extract certificate from issuerAuth x5chain mdoc", e);
        }
        if (cert == null) {
            throw new VerificationException("invalid_request", "Failed to extract certificate from issuerAuth x5chain mdoc since certificate is null");
        }
        return cert;
    }


    protected StatuslistEntry extractStatuslistUriAndIdx(MDoc mDoc) {
        if (!Objects.isNull(mDoc.getMSO()) && !Objects.isNull(mDoc.getMSO().getStatus()) && !Objects.isNull(mDoc.getMSO().getStatus().getStatusList())) {
            return new StatuslistEntry(mDoc.getMSO().getStatus().getStatusList().toJSON().get("idx").toString(), URI.create(mDoc.getMSO().getStatus().getStatusList().getUri()));
        }
        return null;
    }

    public @NonNull String getValidationDetail(ValidationStatus status) {
        return switch (status) {
            case INCONCLUSIVE -> "mdoc: validering feila";
            case VALID -> "mdoc: mdoc er gyldig";
            case INVALID -> "mdoc: mdoc er ugyldig";
            default -> "mdoc: ukjent status";
        };
    }
}
