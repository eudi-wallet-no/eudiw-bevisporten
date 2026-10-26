package no.idporten.eudiw.verifier.statuslist;

import id.walt.mdoc.COSECryptoProviderKeyInfo;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.cose.COSESign1;
import id.walt.mdoc.dataelement.ByteStringElement;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.dataelement.ListElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.MapKey;
import id.walt.mdoc.dataelement.NumberElement;
import id.walt.mdoc.dataelement.StringElement;
import kotlinx.serialization.SerializationException;
import no.idporten.eudiw.verifier.VerificationException;
import org.cose.java.AlgorithmID;

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
import java.util.List;

public class StatusListCwtValidator {
    private static final String ERROR_CODE = "invalid_request";
    private static final String STATUS_LIST_CWT_MEDIA_TYPE = "application/statuslist+cwt";
    static final int COSE_MAC0_TAG = 17;
    static final int COSE_SIGN1_TAG = 18;
    static final int COSE_CONTENT_TYPE_HEADER = 16;
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

        COSESign1 coseSign1 = parseCwt(statusListCwt);
        validateProtectedHeader(coseSign1);
        verifySignature(coseSign1);

        MapElement claims = decodePayload(coseSign1);
        String subject = requiredStringClaim(claims, CWT_SUBJECT_CLAIM, "sub");
        if (!expectedUri.toString().equals(subject)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT sub does not match expected status list uri");
        }

        Instant issuedAt = requiredTimeClaim(claims, CWT_ISSUED_AT_CLAIM, "iat");
        if (issuedAt.isAfter(now.plus(clockSkew))) {
            throw new VerificationException(ERROR_CODE, "Status list CWT iat is in the future");
        }

        DataElement expirationClaim = claims.getValue().get(new MapKey(CWT_EXPIRATION_CLAIM));
        if (expirationClaim != null) {
            Instant expirationTime = timeClaim(expirationClaim, "exp");
            if (!now.minus(clockSkew).isBefore(expirationTime)) {
                throw new VerificationException(ERROR_CODE, "Status list CWT is expired");
            }
        }

        MapElement statusList = requiredMapClaim(claims, CWT_STATUS_LIST_CLAIM, "status_list");
        DataElement bits = statusList.getValue().get(new MapKey(STATUS_LIST_BITS));
        DataElement lst = statusList.getValue().get(new MapKey(STATUS_LIST_DATA));
        Object bitsClaim = bits instanceof NumberElement numberElement ? numberElement.getValue() : bits;
        Object lstClaim = lst instanceof ByteStringElement byteStringElement
                ? byteStringElement.getValue()
                : lst;
        return StatusListDecoder.resolveStatus(idx, bitsClaim, lstClaim);
    }

    COSESign1 parseCwt(byte[] statusListCwt) {
        if (!hasCoseSign1Tag(statusListCwt)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT must be tagged COSE_Sign1");
        }
        try {
            ListElement coseSign1Data = DataElement.Companion.<ListElement>fromCBOR(statusListCwt);
            return new COSESign1(coseSign1Data.getValue());
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

    void validateProtectedHeader(COSESign1 coseSign1) {
        final MapElement protectedHeader;
        try {
            protectedHeader = coseSign1.decodeProtectedHeader();
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT protected header could not be parsed", e);
        }

        DataElement contentType = protectedHeader.getValue().get(new MapKey(COSE_CONTENT_TYPE_HEADER));
        if (!(contentType instanceof StringElement contentTypeElement)
                || !STATUS_LIST_CWT_MEDIA_TYPE.equals(contentTypeElement.getValue())) {
            throw new VerificationException(ERROR_CODE, "Status list CWT protected type must be application/statuslist+cwt");
        }
        if (algorithm(coseSign1.getAlgorithm()) == null) {
            throw new VerificationException(ERROR_CODE, "Status list CWT algorithm is not allowed");
        }
    }

    private AlgorithmID algorithm(int algorithmId) {
        return switch (algorithmId) {
            case -7 -> AlgorithmID.ECDSA_256;
            case -35 -> AlgorithmID.ECDSA_384;
            case -36 -> AlgorithmID.ECDSA_512;
            case -37 -> AlgorithmID.RSA_PSS_256;
            case -38 -> AlgorithmID.RSA_PSS_384;
            case -39 -> AlgorithmID.RSA_PSS_512;
            default -> null;
        };
    }

    private void verifySignature(COSESign1 coseSign1) {
        List<byte[]> encodedChain = coseSign1.getX5Chain();
        if (encodedChain == null || encodedChain.isEmpty()) {
            throw new VerificationException(ERROR_CODE, "Status list CWT must include x5chain");
        }

        List<X509Certificate> certificateChain = encodedChain.stream()
                .map(this::parseCertificate)
                .toList();
        PublicKey publicKey = certificateChain.getFirst().getPublicKey();
        if (!(publicKey instanceof ECPublicKey || publicKey instanceof RSAPublicKey)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT x5chain certificate must contain an EC or RSA public key");
        }

        AlgorithmID algorithm = algorithm(coseSign1.getAlgorithm());
        String keyId = "status-list";
        COSECryptoProviderKeyInfo keyInfo = new COSECryptoProviderKeyInfo(
                keyId,
                algorithm,
                publicKey,
                null,
                certificateChain,
                List.of());
        try {
            SimpleCOSECryptoProvider cryptoProvider = new SimpleCOSECryptoProvider(List.of(keyInfo));
            if (!cryptoProvider.verify1(coseSign1, keyId)) {
                throw new VerificationException(ERROR_CODE, "Status list CWT signature validation failed");
            }
        } catch (VerificationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT signature validation failed", e);
        }
    }

    private X509Certificate parseCertificate(byte[] encodedCertificate) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(encodedCertificate));
        } catch (CertificateException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT x5chain certificate could not be parsed", e);
        }
    }

    MapElement decodePayload(COSESign1 coseSign1) {
        try {
            MapElement claims = coseSign1.decodePayload();
            if (claims == null) {
                throw new VerificationException(ERROR_CODE, "Status list CWT payload is missing");
            }
            return claims;
        } catch (SerializationException | ClassCastException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT claims could not be parsed", e);
        }
    }

    private String requiredStringClaim(MapElement claims, int claimId, String claimName) {
        DataElement claim = claims.getValue().get(new MapKey(claimId));
        if (!(claim instanceof StringElement stringElement) || stringElement.getValue().isBlank()) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required %s claim".formatted(claimName));
        }
        return stringElement.getValue();
    }

    private Instant requiredTimeClaim(MapElement claims, int claimId, String claimName) {
        DataElement claim = claims.getValue().get(new MapKey(claimId));
        if (claim == null) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required %s claim".formatted(claimName));
        }
        return timeClaim(claim, claimName);
    }

    private Instant timeClaim(DataElement claim, String claimName) {
        long epochSeconds = requiredLong(claim, claimName);
        try {
            return Instant.ofEpochSecond(epochSeconds);
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT %s claim is invalid".formatted(claimName), e);
        }
    }

    private MapElement requiredMapClaim(MapElement claims, int claimId, String claimName) {
        DataElement claim = claims.getValue().get(new MapKey(claimId));
        if (!(claim instanceof MapElement mapElement)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required %s claim".formatted(claimName));
        }
        return mapElement;
    }

    private long requiredLong(DataElement value, String claimName) {
        if (!(value instanceof NumberElement numberElement)
                || !(numberElement.getValue() instanceof Byte || numberElement.getValue() instanceof Short
                || numberElement.getValue() instanceof Integer || numberElement.getValue() instanceof Long)) {
            throw new VerificationException(ERROR_CODE, "%s must be an integer".formatted(claimName));
        }
        long integer = numberElement.getValue().longValue();
        if (integer < 0) {
            throw new VerificationException(ERROR_CODE, "%s must be a non-negative integer".formatted(claimName));
        }
        return integer;
    }
}
