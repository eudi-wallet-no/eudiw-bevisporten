package no.idporten.eudiw.verifier.statuslist;

import id.walt.cose.CoseCertificate;
import id.walt.cose.CoseContentType;
import id.walt.cose.CoseHeaders;
import id.walt.cose.CoseInternalKt;
import id.walt.cose.CoseSign1;
import kotlinx.serialization.cbor.CborByteString;
import kotlinx.serialization.cbor.CborElement;
import kotlinx.serialization.cbor.CborElementKt;
import kotlinx.serialization.cbor.CborInteger;
import kotlinx.serialization.cbor.CborMap;
import kotlinx.serialization.cbor.CborString;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.crypto.CoseSignatureVerifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class StatusListCwtValidator {
    private static final String ERROR_CODE = "invalid_request";
    private static final String STATUS_LIST_CWT_MEDIA_TYPE = "application/statuslist+cwt";
    static final int COSE_MAC0_TAG = 17;
    static final int COSE_SIGN1_TAG = 18;
    static final int CWT_SUBJECT_CLAIM = 2;
    static final int CWT_EXPIRATION_CLAIM = 4;
    static final int CWT_ISSUED_AT_CLAIM = 6;
    static final int CWT_TIME_TO_LIVE_CLAIM = 65534;
    static final int CWT_STATUS_LIST_CLAIM = 65533;
    static final String STATUS_LIST_BITS = "bits";
    static final String STATUS_LIST_DATA = "lst";

    private final Duration clockSkew;

    public StatusListCwtValidator(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    public int validateAndResolveStatus(URI expectedUri, int idx, byte[] statusListCwt, Instant now) {
        if (expectedUri == null) {
            throw new VerificationException(ERROR_CODE, "Expected status list uri is required");
        }
        if (statusListCwt == null || statusListCwt.length == 0) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is required");
        }
        if (now == null) {
            throw new VerificationException(ERROR_CODE, "Current time is required");
        }

        CoseSign1 coseSign1 = parseCwt(statusListCwt);
        validateProtectedHeader(coseSign1);
        verifySignature(coseSign1);

        CborMap claims = decodePayload(coseSign1);
        String subject = requiredStringClaim(claims, CWT_SUBJECT_CLAIM, "sub");
        if (!expectedUri.toString().equals(subject)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT sub does not match expected status list uri");
        }

        Instant issuedAt = requiredTimeClaim(claims, CWT_ISSUED_AT_CLAIM, "iat");
        if (issuedAt.isAfter(now.plus(clockSkew))) {
            throw new VerificationException(ERROR_CODE, "Status list CWT iat is in the future");
        }

        CborElement expirationClaim = claims.get(CWT_EXPIRATION_CLAIM);
        if (expirationClaim != null) {
            Instant expirationTime = timeClaim(expirationClaim, "exp");
            if (!now.minus(clockSkew).isBefore(expirationTime)) {
                throw new VerificationException(ERROR_CODE, "Status list CWT is expired");
            }
        }

        CborMap statusList = requiredMapClaim(claims, CWT_STATUS_LIST_CLAIM, "status_list");
        CborElement bits = statusList.get(STATUS_LIST_BITS);
        CborElement lst = statusList.get(STATUS_LIST_DATA);
        Object bitsClaim = bits instanceof CborInteger integer ? CborElementKt.getLongOrNull(integer) : bits;
        Object lstClaim = lst instanceof CborByteString byteString
                ? byteString.getBytes()
                : lst;
        return StatusListDecoder.resolveStatus(idx, bitsClaim, lstClaim);
    }

    CoseSign1 parseCwt(byte[] statusListCwt) {
        if (!hasCoseSign1Tag(statusListCwt)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT must be tagged COSE_Sign1");
        }
        try {
            return CoseSign1.Companion.fromTagged(statusListCwt);
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is not a valid COSE_Sign1", e);
        }
    }

    private boolean hasCoseSign1Tag(byte[] statusListCwt) {
        if (statusListCwt.length == 0) {
            return false;
        }
        int initialByte = Byte.toUnsignedInt(statusListCwt[0]);
        if ((initialByte >>> 5) != 6) {
            return false;
        }
        int additionalInformation = initialByte & 0x1f;
        if (additionalInformation < 24) {
            return additionalInformation == COSE_SIGN1_TAG;
        }

        int length = switch (additionalInformation) {
            case 24 -> 1;
            case 25 -> 2;
            case 26 -> 4;
            case 27 -> 8;
            default -> 0;
        };
        if (length == 0 || statusListCwt.length < length + 1) {
            return false;
        }
        long tag = 0;
        for (int index = 1; index <= length; index++) {
            tag = (tag << 8) | Byte.toUnsignedLong(statusListCwt[index]);
        }
        return tag == COSE_SIGN1_TAG;
    }

    void validateProtectedHeader(CoseSign1 coseSign1) {
        final CoseHeaders protectedHeader;
        try {
            protectedHeader = protectedHeader(coseSign1);
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT protected header could not be parsed", e);
        }

        if (!(protectedHeader.getType() instanceof CoseContentType.AsString type)
                || !STATUS_LIST_CWT_MEDIA_TYPE.equals(type.getValue())) {
            throw new VerificationException(ERROR_CODE, "Status list CWT protected type must be application/statuslist+cwt");
        }
        if (!CoseSignatureVerifier.ALLOWED_ALGORITHMS.contains(protectedHeader.getAlgorithm())) {
            throw new VerificationException(ERROR_CODE, "Status list CWT algorithm is not allowed");
        }
    }

    private CoseHeaders protectedHeader(CoseSign1 coseSign1) {
        return CoseInternalKt.getCoseCompliantCbor()
                .decodeFromByteArray(CoseHeaders.Companion.serializer(), coseSign1.getProtected());
    }

    private void verifySignature(CoseSign1 coseSign1) {
        List<byte[]> encodedChain = x5Chain(coseSign1);
        if (encodedChain.isEmpty()) {
            String keyReferences = keyReferenceHeaders(coseSign1);
            if (!keyReferences.isEmpty()) {
                throw new VerificationException(ERROR_CODE, "Status list CWT key resolution by " + keyReferences
                        + " is not supported yet, x5chain is required");
            }
            throw new VerificationException(ERROR_CODE, "Status list CWT must include x5chain");
        }

        List<X509Certificate> certificateChain = encodedChain.stream()
                .map(this::parseCertificate)
                .toList();
        PublicKey publicKey = certificateChain.getFirst().getPublicKey();
        if (!(publicKey instanceof ECPublicKey || publicKey instanceof RSAPublicKey)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT x5chain certificate must contain an EC or RSA public key");
        }

        try {
            if (!CoseSignatureVerifier.verify(coseSign1, certificateChain.getFirst())) {
                throw new VerificationException(ERROR_CODE, "Status list CWT signature validation failed");
            }
        } catch (VerificationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT signature validation failed", e);
        }
    }

    private List<CoseHeaders> headers(CoseSign1 coseSign1) {
        List<CoseHeaders> headers = new ArrayList<>();
        try {
            headers.add(protectedHeader(coseSign1));
            if (coseSign1.getUnprotected() != null) {
                headers.add(coseSign1.getUnprotected());
            }
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT headers could not be parsed", e);
        }
        return headers;
    }

    private String keyReferenceHeaders(CoseSign1 coseSign1) {
        List<CoseHeaders> headers = headers(coseSign1);
        List<String> found = new ArrayList<>();
        if (headers.stream().anyMatch(header -> header.getKid() != null)) {
            found.add("kid");
        }
        if (headers.stream().anyMatch(header -> header.getX5t() != null)) {
            found.add("x5t");
        }
        if (headers.stream().anyMatch(header -> header.getX5u() != null)) {
            found.add("x5u");
        }
        return String.join("/", found);
    }

    // x5chain (RFC 9360) may be in the protected or unprotected header, and is a single bstr when it holds one certificate.
    private List<byte[]> x5Chain(CoseSign1 coseSign1) {
        for (CoseHeaders header : headers(coseSign1)) {
            if (header.getX5chain() != null && !header.getX5chain().isEmpty()) {
                return header.getX5chain().stream().map(CoseCertificate::getRawBytes).toList();
            }
        }
        return List.of();
    }

    private X509Certificate parseCertificate(byte[] encodedCertificate) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(encodedCertificate));
        } catch (CertificateException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT x5chain certificate could not be parsed", e);
        }
    }

    CborMap decodePayload(CoseSign1 coseSign1) {
        if (coseSign1.getPayload() == null) {
            throw new VerificationException(ERROR_CODE, "Status list CWT payload is missing");
        }
        try {
            CborElement claims = CoseInternalKt.getCoseCompliantCbor()
                    .decodeFromByteArray(CborElement.Companion.serializer(), coseSign1.getPayload());
            if (!(claims instanceof CborMap claimsMap)) {
                throw new VerificationException(ERROR_CODE, "Status list CWT claims could not be parsed");
            }
            return claimsMap;
        } catch (IllegalArgumentException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT claims could not be parsed", e);
        }
    }

    private String requiredStringClaim(CborMap claims, int claimId, String claimName) {
        CborElement claim = claims.get(claimId);
        if (!(claim instanceof CborString stringElement) || stringElement.getValue().isBlank()) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required %s claim".formatted(claimName));
        }
        return stringElement.getValue();
    }

    private Instant requiredTimeClaim(CborMap claims, int claimId, String claimName) {
        CborElement claim = claims.get(claimId);
        if (claim == null) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required %s claim".formatted(claimName));
        }
        return timeClaim(claim, claimName);
    }

    private Instant timeClaim(CborElement claim, String claimName) {
        long epochSeconds = requiredLong(claim, claimName);
        try {
            return Instant.ofEpochSecond(epochSeconds);
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT %s claim is invalid".formatted(claimName), e);
        }
    }

    private CborMap requiredMapClaim(CborMap claims, int claimId, String claimName) {
        CborElement claim = claims.get(claimId);
        if (!(claim instanceof CborMap mapElement)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required %s claim".formatted(claimName));
        }
        return mapElement;
    }

    private long requiredLong(CborElement value, String claimName) {
        if (!(value instanceof CborInteger integer)) {
            throw new VerificationException(ERROR_CODE, "%s must be an integer".formatted(claimName));
        }
        if (!integer.isPositive()) {
            throw new VerificationException(ERROR_CODE, "%s must be a non-negative integer".formatted(claimName));
        }
        Long longValue = CborElementKt.getLongOrNull(integer);
        if (longValue == null) {
            throw new VerificationException(ERROR_CODE, "%s is out of range".formatted(claimName));
        }
        return longValue;
    }
}
