package no.idporten.eudiw.verifier.statuslist;

import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import com.nimbusds.jose.util.X509CertUtils;
import no.idporten.eudiw.verifier.VerificationException;
import org.cose.java.AlgorithmID;
import org.cose.java.Attribute;
import org.cose.java.CoseException;
import org.cose.java.Message;
import org.cose.java.MessageTag;
import org.cose.java.OneKey;
import org.cose.java.Sign1Message;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public class StatusListCwtValidator {
    private static final String ERROR_CODE = "invalid_request";
    private static final String STATUS_LIST_CWT_MEDIA_TYPE = "application/statuslist+cwt";
    private static final int STATUS_LIST_CLAIM = 65533;
    private static final int X5CHAIN_HEADER = 33;
    private static final Set<AlgorithmID> ALLOWED_ALGORITHMS = Set.of(
            AlgorithmID.ECDSA_256,
            AlgorithmID.ECDSA_384,
            AlgorithmID.ECDSA_512,
            AlgorithmID.RSA_PSS_256,
            AlgorithmID.RSA_PSS_384,
            AlgorithmID.RSA_PSS_512);

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

        Sign1Message cwt = parseCwt(statusListCwt);
        validateHeader(cwt);
        verifySignature(cwt);

        CBORObject claims = parseClaims(cwt);
        CBORObject subject = claims.get(2);
        if (subject == null || subject.getType() != CBORType.TextString || subject.AsString().isBlank()) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required sub claim");
        }
        if (!expectedUri.toString().equals(subject.AsString())) {
            throw new VerificationException(ERROR_CODE, "Status list CWT sub does not match expected status list uri");
        }

        Instant issuedAt = requiredTimeClaim(claims, 6, "iat");
        if (issuedAt.isAfter(now.plus(clockSkew))) {
            throw new VerificationException(ERROR_CODE, "Status list CWT iat is in the future");
        }

        CBORObject expirationClaim = claims.get(4);
        if (expirationClaim != null && !expirationClaim.isNull()) {
            Instant expirationTime = timeClaim(expirationClaim, "exp");
            if (!now.minus(clockSkew).isBefore(expirationTime)) {
                throw new VerificationException(ERROR_CODE, "Status list CWT is expired");
            }
        }

        CBORObject statusList = claims.get(STATUS_LIST_CLAIM);
        if (statusList == null || statusList.getType() != CBORType.Map) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required status_list claim");
        }

        int bits = requiredInteger(statusList.get("bits"), "status_list.bits");
        CBORObject lst = statusList.get("lst");
        if (lst == null || lst.getType() != CBORType.ByteString) {
            throw new VerificationException(ERROR_CODE, "status_list.lst must be a CBOR byte string");
        }
        return StatusListDecoder.resolveStatus(idx, bits, lst.GetByteString());
    }

    private Sign1Message parseCwt(byte[] statusListCwt) {
        try {
            CBORObject encodedCwt = CBORObject.DecodeFromBytes(statusListCwt);
            if (!encodedCwt.HasOneTag(18)) {
                throw new VerificationException(ERROR_CODE, "Status list CWT must be tagged COSE_Sign1");
            }
            Message message = Message.DecodeFromBytes(statusListCwt, MessageTag.Sign1);
            if (!(message instanceof Sign1Message sign1Message)) {
                throw new VerificationException(ERROR_CODE, "Status list CWT must be COSE_Sign1");
            }
            return sign1Message;
        } catch (CoseException | RuntimeException e) {
            if (e instanceof VerificationException verificationException) {
                throw verificationException;
            }
            throw new VerificationException(ERROR_CODE, "Status list CWT is not a valid COSE_Sign1", e);
        }
    }

    private void validateHeader(Sign1Message cwt) {
        CBORObject contentType = cwt.findAttribute(CBORObject.FromObject(16), Attribute.PROTECTED);
        if (contentType == null || contentType.getType() != CBORType.TextString
                || !STATUS_LIST_CWT_MEDIA_TYPE.equals(contentType.AsString())) {
            throw new VerificationException(ERROR_CODE, "Status list CWT protected type must be application/statuslist+cwt");
        }

        CBORObject algorithmValue = cwt.findAttribute(CBORObject.FromObject(1), Attribute.PROTECTED);
        if (algorithmValue == null) {
            throw new VerificationException(ERROR_CODE, "Status list CWT protected algorithm is required");
        }
        final AlgorithmID algorithm;
        try {
            algorithm = AlgorithmID.FromCBOR(algorithmValue);
        } catch (CoseException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT algorithm is invalid", e);
        }
        if (!ALLOWED_ALGORITHMS.contains(algorithm)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT algorithm is not allowed");
        }
    }

    private void verifySignature(Sign1Message cwt) {
        CBORObject x5chain = cwt.findAttribute(CBORObject.FromObject(X5CHAIN_HEADER));
        byte[] encodedCertificate = firstCertificate(x5chain);
        X509Certificate certificate = X509CertUtils.parse(encodedCertificate);
        if (certificate == null) {
            throw new VerificationException(ERROR_CODE, "Status list CWT x5chain certificate could not be parsed");
        }
        PublicKey publicKey = certificate.getPublicKey();
        if (!(publicKey instanceof ECPublicKey || publicKey instanceof RSAPublicKey)) {
            throw new VerificationException(ERROR_CODE, "Status list CWT x5chain certificate must contain an EC or RSA public key");
        }
        try {
            if (!cwt.validate(new OneKey(publicKey, null))) {
                throw new VerificationException(ERROR_CODE, "Status list CWT signature validation failed");
            }
        } catch (CoseException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT signature validation failed", e);
        }
    }

    private byte[] firstCertificate(CBORObject x5chain) {
        if (x5chain == null) {
            throw new VerificationException(ERROR_CODE, "Status list CWT must include x5chain");
        }
        if (x5chain.getType() == CBORType.ByteString) {
            return x5chain.GetByteString();
        }
        if (x5chain.getType() == CBORType.Array && x5chain.size() > 0
                && x5chain.get(0).getType() == CBORType.ByteString) {
            return x5chain.get(0).GetByteString();
        }
        throw new VerificationException(ERROR_CODE, "Status list CWT x5chain must contain a certificate");
    }

    private CBORObject parseClaims(Sign1Message cwt) {
        byte[] payload = cwt.GetContent();
        if (payload == null || payload.length == 0) {
            throw new VerificationException(ERROR_CODE, "Status list CWT payload is missing");
        }
        try {
            CBORObject claims = CBORObject.DecodeFromBytes(payload);
            if (claims.getType() != CBORType.Map) {
                throw new VerificationException(ERROR_CODE, "Status list CWT claims must be a CBOR map");
            }
            return claims;
        } catch (RuntimeException e) {
            if (e instanceof VerificationException verificationException) {
                throw verificationException;
            }
            throw new VerificationException(ERROR_CODE, "Status list CWT claims could not be parsed", e);
        }
    }

    private Instant requiredTimeClaim(CBORObject claims, int claimId, String claimName) {
        CBORObject claim = claims.get(claimId);
        if (claim == null) {
            throw new VerificationException(ERROR_CODE, "Status list CWT is missing required %s claim".formatted(claimName));
        }
        return timeClaim(claim, claimName);
    }

    private Instant timeClaim(CBORObject claim, String claimName) {
        long epochSeconds = requiredLong(claim, claimName);
        try {
            return Instant.ofEpochSecond(epochSeconds);
        } catch (RuntimeException e) {
            throw new VerificationException(ERROR_CODE, "Status list CWT %s claim is invalid".formatted(claimName), e);
        }
    }

    private int requiredInteger(CBORObject value, String claimName) {
        long integer = requiredLong(value, claimName);
        if (integer < 0 || integer > Integer.MAX_VALUE) {
            if ("status_list.bits".equals(claimName)) {
                throw new VerificationException(ERROR_CODE, "%s must be a valid status-list bit size".formatted(claimName));
            }
            throw new VerificationException(ERROR_CODE, "%s must be a non-negative integer".formatted(claimName));
        }
        return (int) integer;
    }

    private long requiredLong(CBORObject value, String claimName) {
        if (value == null || value.getType() != CBORType.Integer || !value.CanValueFitInInt64()) {
            throw new VerificationException(ERROR_CODE, "%s must be an integer".formatted(claimName));
        }
        try {
            return value.AsInt64Value();
        } catch (ArithmeticException e) {
            throw new VerificationException(ERROR_CODE, "%s must be an integer".formatted(claimName), e);
        }
    }
}
