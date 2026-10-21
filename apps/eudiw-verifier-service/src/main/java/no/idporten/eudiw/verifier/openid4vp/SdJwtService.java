package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONArrayUtils;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.sdjwt.*;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonNull;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.crypto.ECUtils;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatusSdJwt;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus.*;

@Service
public class SdJwtService {
    private static final Logger log = LogManager.getLogger(SdJwtService.class);
    private final ObjectMapper objectMapper;
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

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

    public ValidationStatus validationStatusSdJwt(VerificationResult<SDJwt> verificationResult, ValidationStatus sdJwtTimeChecksStatus) {
        if (!verificationResult.getVerified() || sdJwtTimeChecksStatus != VALID) {
            return INVALID;
        } else {
            return VALID;
        }
    }

    public ValidationStatus validationStatusHolderBinding(VerificationTransaction verificationTransaction, SDJwt sdJwt, Boolean isCryptographicHolderBindingRequired) {
        if (!checkHolderBinding(verificationTransaction, sdJwt, isCryptographicHolderBindingRequired)) {
            throw new VerificationException(
                    "invalid_request",
                    "Invalid vp_token. Holder binding failed.");
        }
        return VALID;
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

    public ValidationStatus validateSdJwtTimeClaims(JsonObject sdJwtPayload) {
        Instant now = Instant.now(Clock.systemUTC());
        log.info("Validating SD-JWT time claims against current time: {} (skew={}s)", now, CLOCK_SKEW.getSeconds());

        // iat is optional for SD-JWT/key binding jwt
        Instant iat = parseInstantClaim(sdJwtPayload, "iat");
        if (iat != null && iat.isAfter(now.plus(CLOCK_SKEW))) {
            log.warn("SD-JWT iat claim is too far in the future: {} (now={}, skew={})", iat, now, CLOCK_SKEW);
            return INVALID;
        }

        // nbf is optional for SD-JWT/key binding jwt
        Instant nbf = parseInstantClaim(sdJwtPayload, "nbf");
        if (nbf != null && nbf.isAfter(now.plus(CLOCK_SKEW))) {
            log.warn("SD-JWT nbf claim is too far in the future: {} (now={}, skew={})", nbf, now, CLOCK_SKEW);
            return INVALID;
        }

        // exp is optional for SD-JWT/key binding jwt
        Instant exp = parseInstantClaim(sdJwtPayload, "exp");
        if (exp != null && !exp.isAfter(now.minus(CLOCK_SKEW))) {
            log.warn("SD-JWT exp claim is too far in the past: {} (now={}, skew={})", exp, now, CLOCK_SKEW);
            return INVALID;
        }

        return ValidationStatus.VALID;

    }

    public ValidationStatus validateSdJwtTimeClaims(VerificationResult<SDJwt> verificationResult) {
        ValidationStatus kbJwtTimeClaimsStatus;
        if (containsKBJwt(verificationResult.getSdJwt())) {
            SDJwt kbJwt = verificationResult.getSdJwt().getKeyBindingJwt();
            kbJwtTimeClaimsStatus = validateSdJwtTimeClaims(kbJwt.getFullPayload());
            if (kbJwtTimeClaimsStatus != VALID) {
                log.warn("Key binding JWT time claims validation failed");
                return INVALID;
            }

        }
       ValidationStatus sdJwtTimeClaimsStatus = validateSdJwtTimeClaims(verificationResult.getSdJwt().getFullPayload());
        if (sdJwtTimeClaimsStatus != VALID) {
            log.warn("SD JWT time claims validation failed");
            return INVALID;
        }
        return VALID;
    }

    private Instant parseInstantClaim(JsonObject payload, String claimName) {
        JsonElement element = payload.get(claimName);
        if (element == null || element == JsonNull.INSTANCE) {
            log.info("SD-JWT instant claim {} is not present or null", claimName);
            return null;
        }

        if (!(element instanceof JsonPrimitive primitive)) {
            log.warn("SD-JWT instant claim {} is not a primitive value", claimName);
            throw new VerificationException("invalid_request",
                    "Invalid %s claim. Expected primitive value".formatted(claimName));
        }

        if (primitive.isString()) {
            try {
                return Instant.parse(primitive.getContent());
            } catch (DateTimeException e) {
                log.warn("SD-JWT instant claim {} is not a valid string", claimName, e);
                throw new VerificationException("invalid_request",
                        "Invalid %s claim. Expected NumericDate or ISO-8601 UTC".formatted(claimName), e);
            }
        }

        // Number is represented as text in kotlinx JsonPrimitive
        try {
            long epochSeconds = Long.parseLong(primitive.getContent());
            return Instant.ofEpochSecond(epochSeconds);
        } catch (NumberFormatException | DateTimeException | ArithmeticException e) {
            throw new VerificationException("invalid_request",
                    "Invalid %s claim. Expected NumericDate within valid Instant range".formatted(claimName), e);
        }
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
        X509Certificate cert;
        try {
            JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
            cert = X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Failed to extract certificate from SDJwt", e);
        }
        if(cert == null) {
            throw new VerificationException("invalid_request", "Failed to extract certificate from SDJwt or certificate is null");
        }
        return cert;
    }

    protected JWSVerifier jwsVerifier(X509Certificate cert) {
        try {
            return new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
        } catch (JOSEException e) {
            throw new VerificationException("invalid_request", "Failed to create JWS verifier for SDJwt", e);
        }
    }

    public boolean containsCnf(SDJwt sdJwt) {
        return sdJwt.getFullPayload().get("cnf") != null;
    }

    public boolean containsKBJwt(SDJwt sdJwt) {
        return sdJwt.getKeyBindingJwt() != null;
    }

    public boolean isHolderBindingPresent(SDJwt sdJwt) {
        return containsCnf(sdJwt) || containsKBJwt(sdJwt) || containsCnf(sdJwt) && containsKBJwt(sdJwt);
    }


    /**
     * Check holder binding by verifying the key binding JWT using the holder's public key from the SD-JWT's "cnf" claim.
     * @param verificationTransaction contains nonce and aud from authorization request.
     * @param sdJwt the SD-JWT containing the key binding JWT and the holder's public key in the "cnf" claim.
     * @return true if the holder binding is valid, false otherwise.
     */
    protected boolean checkHolderBinding(VerificationTransaction verificationTransaction, SDJwt sdJwt, Boolean isCryptographicHolderBindingRequired) {
        if (isCryptographicHolderBindingRequired || isHolderBindingPresent(sdJwt)) {
            Object cnfRaw = sdJwt.getFullPayload().get("cnf");
            Map<String, Object> cnf = (Map<String, Object>) cnfRaw;
            if(cnf == null || cnf.get("jwk") == null) {
                throw new VerificationException("invalid_request", "Missing cnf.jwk claim in SDJwt");
            }
            ECPublicKey holderKey = jwkToEcPublicKey(cnf);
            if(holderKey == null) {
                throw new VerificationException("invalid_request", "Invalid holder public key in cnf.jwk claim");
            }
            try {
                JWSVerifier verifier = new ECDSAVerifier(holderKey);
                SimpleJWTCryptoProvider cryptoProviderHolderBinding =
                        new SimpleJWTCryptoProvider(ECUtils.jwsAlgorithmFromKey(holderKey), null, verifier);
                return sdJwt.getKeyBindingJwt().verifyKB(cryptoProviderHolderBinding, verificationTransaction.getAudience(), verificationTransaction.getNonce(), sdJwt, null);
            } catch (JOSEException e) {
                throw new VerificationException("invalid_request", "Failed to create JWS verifier for holder binding", e);
            }
        }
        return true;
    }

    private ECPublicKey jwkToEcPublicKey(Map<String, Object> jwk) {
        try {
            if (jwk == null) {
                throw new IllegalArgumentException("cnf.jwk missing");
            }
            JWK parsedJwk = JWK.parse(jwk.get("jwk").toString());
            ECKey ecKey = parsedJwk.toECKey();
            return ecKey.toECPublicKey();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse cnf JWK", e);
        } catch (JOSEException e) {
            throw new IllegalArgumentException("Failed to convert JWK to EC public key", e);
        }
    }

    protected JWSAlgorithm algorithm(X509Certificate cert) {
        return ECUtils.jwsAlgorithmFromKey(cert.getPublicKey());
    }

    public @NonNull String getValidationDetailSDJWT(ValidationStatus status) {
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

   public @NonNull String getValidationDetailHolderBinding(ValidationStatus status) {
        switch (status) {
            case INCONCLUSIVE:
                return "Holder binding: validering feila";
            case VALID:
                return "Holder binding: gyldig";
            case INVALID:
                return "Holder binding: ugyldig";
            default:
                return "Holder binding: ukjent status";
        }
    }
}
