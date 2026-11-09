package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONArrayUtils;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.credentials.formats.DigitalCredential;
import id.walt.credentials.formats.SdJwtCredential;
import id.walt.credentials.presentations.formats.DcSdJwtPresentation;
import id.walt.policies2.vp.policies.*;
import id.walt.policies2.vp.policies.VPPolicy2.PolicyRunResult;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.config.SdjwtConfig;
import no.idporten.eudiw.verifier.crypto.JwsSignatureVerifier;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.walt.WaltPresentations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Verifies SD-JWT VC presentations. Holder binding is verified with the key binding JWT. The issuer key is locked to
 * the x5c certificate, since walt.id would also resolve it from a DID in iss or from .well-known.
 */
@Service
public class SdJwtCredentialVerifier extends CredentialVerifier {

    /** Policies that verify the key binding JWT, i.e. holder binding. */
    static final Set<String> HOLDER_BINDING_POLICIES = Set.of(
            "dc+sd-jwt/kb-jwt_signature",
            "dc+sd-jwt/nonce-check",
            "dc+sd-jwt/audience-check",
            "dc+sd-jwt/sd_hash-check",
            "dc+sd-jwt/kb-jwt_iat-check");

    private static final Predicate<String> HOLDER_BINDING_POLICY = HOLDER_BINDING_POLICIES::contains;

    private final List<DcSdJwtVPPolicy> policies;

    @Autowired
    public SdJwtCredentialVerifier(SdjwtConfig sdjwtConfig) {
        this(Clock.systemUTC(), defaultPolicies(sdjwtConfig.clockSkew()));
    }

    SdJwtCredentialVerifier(Clock clock, List<DcSdJwtVPPolicy> policies) {
        super(clock);
        this.policies = policies;
    }

    static List<DcSdJwtVPPolicy> defaultPolicies(Duration clockSkew) {
        long clockSkewSeconds = clockSkew.toSeconds();
        return List.of(
                new KbJwtSignatureSdJwtVPPolicy(),
                new NonceCheckSdJwtVPPolicy(),
                new AudienceCheckSdJwtVPPolicy(),
                new SdHashCheckSdJwtVPPolicy(),
                new KbJwtIatCheckSdJwtVPPolicy(),
                new ExpCheckSdJwtVPPolicy(clockSkewSeconds),
                new NbfCheckSdJwtVPPolicy(clockSkewSeconds));
    }

    @Override
    public CredentialVerificationResult verify(String vpToken, DcqlCredentialQuery credentialQuery, VerificationTransaction transaction) {
        // A presentation without key binding JWT ends with the separator after the last disclosure
        boolean hasKeyBinding = !vpToken.endsWith("~");
        if (!hasKeyBinding && credentialQuery.getRequireCryptographicHolderBinding()) {
            throw holderBindingFailed();
        }
        DcSdJwtPresentation presentation = hasKeyBinding
                ? parse(() -> WaltPresentations.parseSdJwtPresentation(vpToken), "Failed to parse SD-JWT vp_token")
                : presentationWithoutKeyBinding(vpToken);

        X509Certificate issuerCertificate = issuerCertificate(presentation.getSdJwt());
        try {
            JwsSignatureVerifier.verify(presentation.getSdJwt(), issuerCertificate);
        } catch (IllegalArgumentException e) {
            throw new VerificationException("invalid_request", "Invalid vp_token. Issuer signature is invalid.", e);
        }

        List<DcSdJwtVPPolicy> selectedPolicies = hasKeyBinding ? policies : without(policies, HOLDER_BINDING_POLICY);
        Map<String, PolicyRunResult> results = runPolicies(presentation,
                new VPPolicyList(List.of(), selectedPolicies, List.of()), vpToken, transaction);

        ValidationStatus holderBindingStatus = hasKeyBinding ? status(results, HOLDER_BINDING_POLICY) : ValidationStatus.VALID;
        if (holderBindingStatus != ValidationStatus.VALID) {
            throw holderBindingFailed();
        }
        JsonObject credentialData = presentation.getCredential().getCredentialData();
        return new CredentialVerificationResult(
                CredentialFormat.DC_SD_JWT,
                stringClaim(credentialData, "vct"),
                disclosureClaims(presentation.getDisclosures()),
                issuerCertificate,
                statuslistEntry(credentialData),
                status(results, HOLDER_BINDING_POLICY.negate()),
                holderBindingStatus);
    }

    // Holder binding is not required, so only the credential policies are run on the issuer-signed part
    private static DcSdJwtPresentation presentationWithoutKeyBinding(String vpToken) {
        DigitalCredential credential = parse(() -> WaltPresentations.parseCredential(vpToken), "Failed to parse SD-JWT vp_token");
        if (!(credential instanceof SdJwtCredential)) {
            throw new VerificationException("invalid_request", "vp_token is not an SD-JWT VC");
        }
        List<String> parts = List.of(vpToken.split("~"));
        return new DcSdJwtPresentation(parts.getFirst(), parts.subList(1, parts.size()), "", credential,
                null, null, null, null, null, "");
    }

    private static X509Certificate issuerCertificate(String issuerSignedJwt) {
        List<Base64> x5c;
        try {
            x5c = JWSHeader.parse(new Base64URL(issuerSignedJwt.substring(0, issuerSignedJwt.indexOf('.')))).getX509CertChain();
        } catch (ParseException | IndexOutOfBoundsException e) {
            throw new VerificationException("invalid_request", "Failed to extract certificate from SDJwt", e);
        }
        X509Certificate certificate = x5c == null || x5c.isEmpty() ? null : X509CertUtils.parse(x5c.getFirst().decode());
        if (certificate == null) {
            throw new VerificationException("invalid_request", "Failed to extract certificate from SDJwt or certificate is null");
        }
        return certificate;
    }

    // Disclosed claims by name, as in earlier versions of the API. Array element disclosures have no name.
    private static Map<String, Object> disclosureClaims(List<String> disclosures) {
        Map<String, Object> claims = new HashMap<>();
        for (String disclosure : disclosures) {
            List<Object> parsedDisclosure;
            try {
                parsedDisclosure = JSONArrayUtils.parse(new String(java.util.Base64.getUrlDecoder().decode(disclosure)));
            } catch (ParseException | IllegalArgumentException e) {
                throw new VerificationException("invalid_request", "Failed to parse disclosure", e);
            }
            if (parsedDisclosure.size() == 3 && parsedDisclosure.get(1) instanceof String claimName) {
                claims.put(claimName, parsedDisclosure.get(2));
            } else if (parsedDisclosure.size() != 2) {
                throw new VerificationException("invalid_request", "Failed to parse disclosure");
            }
        }
        return claims;
    }

    private static StatuslistEntry statuslistEntry(JsonObject credentialData) {
        if (!(credentialData.get("status") instanceof JsonObject status)
                || !(status.get("status_list") instanceof JsonObject statusList)) {
            return null;
        }
        if (!(statusList.get("idx") instanceof JsonPrimitive idx) || !(statusList.get("uri") instanceof JsonPrimitive uri)) {
            throw new VerificationException("invalid_request", "Invalid status list in vp_token");
        }
        return new StatuslistEntry(idx.getContent(), URI.create(uri.getContent()));
    }

    private static String stringClaim(JsonObject data, String name) {
        return data.get(name) instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getContent() : null;
    }
}
