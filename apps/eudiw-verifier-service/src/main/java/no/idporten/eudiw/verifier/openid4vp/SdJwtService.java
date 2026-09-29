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
import jakarta.servlet.http.HttpSession;
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
import java.util.*;

@Service
public class SdJwtService {
    private static final Logger log = LogManager.getLogger(SdJwtService.class);
    private final ObjectMapper objectMapper;

    public SdJwtService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public SDJwt sdJwtFromVpToken(String vpToken) {
        return SDJwt.Companion.parse(vpToken);
    }

    public VerificationResult<SDJwt> verifySdJwt(SDJwt sdJwt, X509Certificate cert, VerificationTransaction verificationTransaction) {
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
        if (!holderBinding(verificationResult.getSdJwt(), verificationTransaction)) {
            throw new VerificationException(
                    "invalid_request",
                    "Invalid vp_token. Holder binding failed.");
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

    public boolean holderBindingRequired(String dcql) {
        return dcql != null && dcql.contains("cnf");
    }

    public boolean holderBinding(SDJwt sdJwt, VerificationTransaction verificationTransaction) {
        return checkHolderBinding(verificationTransaction, sdJwt);
    }


    /**
     * Check holder binding by verifying the key binding JWT using the holder's public key from the SD-JWT's "cnf" claim.
     * @param verificationTransaction contains nonce and aud from authorization request.
     * @param sdJwt the SD-JWT containing the key binding JWT and the holder's public key in the "cnf" claim.
     * @return true if the holder binding is valid, false otherwise.
     */
    protected boolean checkHolderBinding(VerificationTransaction verificationTransaction, SDJwt sdJwt) {

        Object cnfRaw = sdJwt.getFullPayload().get("cnf");
        Map<String, Object> cnf = (Map<String, Object>) cnfRaw;
        Map<String, Object> jwk = (Map<String, Object>) cnf.get("jwk");

        ECPublicKey holderKey = jwkToEcPublicKey(jwk);
        try {
            JWSVerifier verifier = new ECDSAVerifier(holderKey);
            SimpleJWTCryptoProvider cryptoProviderHolderBinding =
                    new SimpleJWTCryptoProvider(JWSAlgorithm.ES256, null, verifier);
            return sdJwt.getKeyBindingJwt().verifyKB(cryptoProviderHolderBinding, verificationTransaction.getAudience(), verificationTransaction.getNonce(), sdJwt, null);
        } catch (JOSEException e) {
            throw new VerificationException("invalid_request", "Failed to create JWS verifier for holder binding", e);
        }
    }

    private static ECPublicKey jwkToEcPublicKey(Map<String, Object> jwk) {
        try {
            if (!(jwk instanceof Map<?, ?> jwkMap)) {
                throw new IllegalArgumentException("cnf.jwk missing");
            }
            JWK parsedJwk = JWK.parse(jwkMap.toString());

            if (!(parsedJwk instanceof ECKey ecKey)) {
                throw new IllegalArgumentException("cnf.jwk is not EC");
            }
            return ecKey.toECPublicKey();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse cnf JWK", e);
        }
    }

    protected JWSAlgorithm algorithm(X509Certificate cert) {
        return ECUtils.jwsAlgorithmFromKey(cert.getPublicKey());
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
