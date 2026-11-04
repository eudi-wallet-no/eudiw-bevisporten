package no.idporten.eudiw.verifier.openid4vp;

import id.walt.cose.CoseCertificate;
import id.walt.cose.CoseHeaders;
import id.walt.cose.CoseInternalKt;
import id.walt.cose.CoseSign1;
import id.walt.mdoc.encoding.ByteStringWrapper;
import id.walt.mdoc.objects.deviceretrieval.DeviceResponse;
import id.walt.mdoc.objects.document.Document;
import id.walt.mdoc.objects.document.IssuerSigned;
import id.walt.mdoc.objects.elements.IssuerSignedItem;
import id.walt.mdoc.objects.elements.IssuerSignedList;
import id.walt.mdoc.objects.mso.MobileSecurityObject;
import id.walt.mdoc.crypto.MdocCrypto;
import id.walt.mdoc.objects.mso.Status;
import id.walt.mdoc.objects.mso.ValidityInfo;
import id.walt.mdoc.verification.IssuerSignedDataIntegrityKt;
import kotlinx.serialization.cbor.*;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.crypto.CoseSignatureVerifier;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MDocService {

    private static final Logger log = LogManager.getLogger(MDocService.class);

    private final Clock clock;

    public MDocService() {
        this(Clock.systemUTC());
    }

    MDocService(Clock clock) {
        this.clock = clock;
    }

    public DeviceResponse deviceResponseFromVpToken(String vpToken) {
        DeviceResponse deviceResponse;
        try {
            byte[] encoded = Base64.getUrlDecoder().decode(vpToken);
            deviceResponse = CoseInternalKt.getCoseCompliantCbor()
                    .decodeFromByteArray(DeviceResponse.Companion.serializer(), encoded);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new VerificationException("invalid_request", "Failed to parse mdoc vp_token", e);
        }
        if (deviceResponse.getDocuments() == null || deviceResponse.getDocuments().length == 0) {
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
    public Document mDocFromDeviceResponse(DeviceResponse deviceResponse) {
        if(deviceResponse.getDocuments().length > 1) {
            throw new VerificationException("invalid_request", "Multiple mdoc documents in vp_token, only one is supported");
        } else {
            return deviceResponse.getDocuments()[0];
        }
    }

    public Map<String, Object> claimsFromMDoc(Document mDoc) {
        Map<String, Object> claims = new HashMap<>();
        mDocClaims(mDoc.getIssuerSigned(), claims);
        return claims;
    }

    /**
     * Verifies issuer signature, MSO validity, doctype and digests of the disclosed items. Device authentication is
     * not verified. The issuer signature is verified with the same certificate as is checked against the trust list.
     */
    protected ValidationStatus verifyMDoc(Document mDoc, VerificationTransaction verificationTransaction) {

        X509Certificate issuerCertificate = extractCertificateFromMdoc(mDoc);

        try {
            MobileSecurityObject mso = mso(mDoc);
            if (!CoseSignatureVerifier.verify(mDoc.getIssuerSigned().getIssuerAuth(), issuerCertificate)) {
                log.warn("mdoc issuer signature is invalid");
                return ValidationStatus.INVALID;
            }
            issuerCertificate.checkValidity(new Date(mso.getValidityInfo().getSigned().toEpochMilliseconds()));
            verifyMsoValidity(mso);
            if (!mDoc.getDocType().equals(mso.getDocType())) {
                log.warn("mdoc doctype {} does not match MSO doctype {}", mDoc.getDocType(), mso.getDocType());
                return ValidationStatus.INVALID;
            }
            IssuerSignedDataIntegrityKt.verifyIssuerSignedItemDigests(mDoc, mso);
            return ValidationStatus.VALID;
        } catch (IllegalArgumentException | IllegalStateException
                 | CertificateExpiredException | CertificateNotYetValidException e) {
            log.warn("mdoc verification failed: {}", e.getMessage());
            return ValidationStatus.INVALID;
        }
    }

    // Same checks as MdocVerifier.verifyMso, but against our clock instead of the system clock
    private void verifyMsoValidity(MobileSecurityObject mso) {
        ValidityInfo validityInfo = mso.getValidityInfo();
        validityInfo.precheck();
        Instant now = clock.instant();
        Instant validFrom = Instant.ofEpochSecond(validityInfo.getValidFrom().getEpochSeconds());
        Instant validUntil = Instant.ofEpochSecond(validityInfo.getValidUntil().getEpochSeconds());
        if (now.isBefore(validFrom)) {
            throw new IllegalArgumentException("MSO is not yet valid, valid from " + validFrom);
        }
        if (now.isAfter(validUntil)) {
            throw new IllegalArgumentException("MSO is no longer valid, expired " + validUntil);
        }
        if (!MdocCrypto.INSTANCE.isSupportedDigest(mso.getDigestAlgorithm())) {
            throw new IllegalArgumentException("MSO digest algorithm is not supported: " + mso.getDigestAlgorithm());
        }
    }

    /**
     * mdoc paths consist of a namespace and an element identifier. The claims are returned as a map of namespace
     * to a map of element identifier to value.
     **/

    protected Map<String, Object> mDocClaims(IssuerSigned issuerSigned, Map<String, Object> claims) {
        if (issuerSigned.getNamespaces() == null) {
            return claims;
        }
        for (Map.Entry<String, IssuerSignedList> namespace : issuerSigned.getNamespaces().entrySet()) {
            Map<String, Object> nameSpaceMap = (Map<String, Object>) claims.computeIfAbsent(namespace.getKey(), _ -> new HashMap<String, Object>());
            for (ByteStringWrapper<IssuerSignedItem> wrappedItem : namespace.getValue().getEntries()) {
                IssuerSignedItem item = wrappedItem.getValue();
                nameSpaceMap.put(item.getElementIdentifier(), extractValue(item.getElementValue()));
            }
        }
        return claims;
    }

    protected Object extractValue(CborElement element) {
        return switch (element) {
            case null -> null;
            case CborBoolean booleanElement -> booleanElement.getValue();
            case CborInteger integerElement -> CborElementKt.getLong(integerElement);
            case CborFloat floatElement -> floatElement.getValue();
            // tdate (tag 0) and full-date (tag 1004) are text strings
            case CborString stringElement -> stringElement.getValue();
            case CborNull _, CborUndefined _ -> null;
            case CborMap mapElement -> mapElement.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(extractValue(e.getKey())),
                            e -> extractValue(e.getValue())));
            case CborArray listElement -> listElement
                    .stream()
                    .map(this::extractValue)
                    .toList();
            case CborByteString byteStringElement -> Base64.getEncoder().encodeToString(byteStringElement.getBytes());
            default -> String.valueOf(element);
        };
    }

    protected X509Certificate extractCertificateFromMdoc(Document mDoc) {
        IssuerSigned issuerSigned = mDoc.getIssuerSigned();
        if (issuerSigned.getIssuerAuth() == null) {
            throw new VerificationException("invalid_request", "issuerAuth is missing in mdoc");
        }
        List<CoseCertificate> x5chain = x5Chain(issuerSigned.getIssuerAuth());
        if (x5chain == null || x5chain.isEmpty()) {
            throw new VerificationException("invalid_request", "x5chain is missing in issuerAuth of mdoc");
        }
        byte[] leafDer = x5chain.getFirst().getRawBytes();
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

    // ISO 18013-5 places x5chain in the unprotected header, but some issuers put it in the protected header.
    private List<CoseCertificate> x5Chain(CoseSign1 issuerAuth) {
        if (issuerAuth.getUnprotected() != null && issuerAuth.getUnprotected().getX5chain() != null) {
            return issuerAuth.getUnprotected().getX5chain();
        }
        try {
            return CoseInternalKt.getCoseCompliantCbor()
                    .decodeFromByteArray(CoseHeaders.Companion.serializer(), issuerAuth.getProtected())
                    .getX5chain();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected StatuslistEntry extractStatuslistUriAndIdx(Document mDoc) {
        Status status = mso(mDoc).getStatus();
        if (status == null || status.getStatusList() == null) {
            return null;
        }
        // idx is an unsigned long and uri a value class, neither has a Java accessible getter
        JsonObject statusList = JsonElementKt.getJsonObject(Json.Default.encodeToJsonElement(
                Status.StatusListInfo.Companion.serializer(), status.getStatusList()));
        return new StatuslistEntry(
                JsonElementKt.getJsonPrimitive(statusList.get("idx")).getContent(),
                URI.create(JsonElementKt.getJsonPrimitive(statusList.get("uri")).getContent()));
    }

    private MobileSecurityObject mso(Document mDoc) {
        try {
            return mDoc.getIssuerSigned().decodeMobileSecurityObject();
        } catch (IllegalArgumentException e) {
            throw new VerificationException("invalid_request", "Unable to parse MSO of mdoc", e);
        }
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
