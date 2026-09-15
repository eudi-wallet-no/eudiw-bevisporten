package no.idporten.eudiw.verifier.openid4vp;

import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
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
public class MDocService  {


    public MDocService() {
    }

    public MDoc mDocFromVpToken(String vpToken) {
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
        id.walt.mdoc.doc.MDoc mdc = deviceResponse.getDocuments().getFirst();
        return mdc;
    }

    public Map<String, Object> claimsFromMDoc(MDoc mDoc) {
        Map<String, Object> claims = new HashMap<>();
        mDocClaims(mDoc.getIssuerSigned(), claims);
        return claims;
    }

    protected ValidationStatus verifyMDoc (id.walt.mdoc.doc.MDoc mDoc) {
        mDoc.getMSO(); // MSO (Mobile Security Object) verification is not performed here because the issuer's public key or certificate is not available in this context.
        // Proper MSO verification is critical for mdoc validation and should be implemented as soon as the issuer's public key can be obtained.
        // Failing to verify the MSO means the authenticity and integrity of the credential cannot be guaranteed.
        // TODO: Implement MSO verification using the issuer's public key or certificate when it becomes available.
        boolean verifyDocType = mDoc.verifyDocType();
        boolean verifyIssuerSignedItems = mDoc.verifyIssuerSignedItems();
        boolean verifyValidity = mDoc.verifyValidity();
        if (verifyDocType && verifyIssuerSignedItems && verifyValidity) {
            return ValidationStatus.VALID;
        } else {
            return ValidationStatus.INVALID;
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
