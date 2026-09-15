package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.util.JSONArrayUtils;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.crypto.ECUtils;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatusSdJwt;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.*;

@Service
public class SdJwtService {
    private final ObjectMapper objectMapper;

    public SdJwtService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public SDJwt sdJwtFromVpToken(String vpToken) {
        return SDJwt.Companion.parse(vpToken);
    }

    public VerificationResult<SDJwt> verifySdJwt(SDJwt sdJwt, X509Certificate cert) {
        JWSVerifier jwsVerifier = jwsVerifier(cert);
        JWSAlgorithm jwsAlgorithm = algorithm(cert);
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(jwsAlgorithm, null, jwsVerifier);
        VerificationResult<SDJwt> verificationResult = sdJwt.verify(cryptoProvider, null);
        if (!verificationResult.getVerified()) {
            throw new VerificationException(
                    "invalid_request",
                    "Invalid vp_token. Signature verified: %s, disclosures verified: %s".formatted(
                            verificationResult.getSignatureVerified(),
                            verificationResult.getDisclosuresVerified()));
        }
        return verificationResult;
    }

    public ValidationStatus validationStatusSdJwt(VerificationResult<SDJwt> verificationResult) {
        if (!verificationResult.getVerified()) {
            return ValidationStatus.INVALID;
        } else {
            return ValidationStatus.VALID;
        }
    }

    public Map<String, Object> sdJwtClaims(VerificationResult<SDJwt> verificationResult) {
        return getClaimsFromSDJwt(verificationResult);
    }

    private static @NonNull Map<String, Object> getClaimsFromSDJwt(VerificationResult<SDJwt> verificationResult) {
        Map<String, Object> claims = new HashMap<>();
        for (String disclosure : verificationResult.getSdJwt().getDisclosures()) {
            List<Object> parsedDisclosure;
            try {
                parsedDisclosure = JSONArrayUtils.parse(new String(Base64.getUrlDecoder().decode(disclosure)));

            } catch (ParseException | IllegalArgumentException e) {
                throw new VerificationException("invalid_request", "Failed to parse disclosure", e);
            }
            if (parsedDisclosure.size() == 2) {
                // Unnamed disclosures are valid for SD-JWT array elements.
                continue;
            }
            if (parsedDisclosure.size() != 3) {
                throw new VerificationException("invalid_request", "Failed to parse disclosure");
            }
            if (parsedDisclosure.get(1) instanceof String claimName) {
                claims.put(claimName, parsedDisclosure.get(2));
                continue;
            }
            throw new VerificationException("invalid_request", "Failed to parse disclosure for claim=%s".formatted(parsedDisclosure.get(1)));
        }
        return claims;
    }

    protected StatuslistEntry extractStatuslistUriAndIdx(VerificationResult<SDJwt> sdjwt) {
        Object statusObj = sdjwt.getSdJwt().getFullPayload().get("status");
        if (Objects.isNull(statusObj) || !StringUtils.hasText(statusObj.toString())) {
            return null;
        }
        StatusSdJwt statusSdJwt = objectMapper.convertValue(statusObj, StatusSdJwt.class);
        return new StatuslistEntry(statusSdJwt.statuslist().idx().content(), URI.create(statusSdJwt.statuslist().uri().content()));
    }


    protected X509Certificate certificate(SDJwt unverifiedSDJwt) {
        try {
            JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
            return X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Failed to extract certificate from SDJwt", e);
        }
    }

    protected JWSVerifier jwsVerifier(X509Certificate cert) {
        try {
            return new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
        } catch (JOSEException e) {
            throw new VerificationException("invalid_request", "Failed to create JWS verifier for SDJwt", e);
        }
    }

    protected JWSAlgorithm algorithm(X509Certificate cert) {
        return ECUtils.jwsAlgorithmFromKey(cert.getPublicKey());
    }

    protected VerificationResult<SDJwt> verificationResult(SimpleJWTCryptoProvider jwtCryptoProvider, SDJwt unverifiedSDJwt) {
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(jwtCryptoProvider, null);
        if (!verificationResult.getVerified()) {
            throw new VerificationException("invalid_request", "Invalid vp_token. Signature verified: %s, disclosures verified: %s".formatted(verificationResult.getSignatureVerified(), verificationResult.getDisclosuresVerified()));
        }
        return verificationResult;
    }

    public @NonNull String getValidationDetail(ValidationStatus status) {
        switch (status) {
            case INCONCLUSIVE:
                return "SD-JWT VC: validering feila";
            case VALID:
                return "SD-JWT VC: SDJwt er gyldig";
            case INVALID:
                return "SD-JWT VC: SDJwt er ugyldig";
            default:
                return "SD-JWT VC: ukjent status";
        }
    }

}
