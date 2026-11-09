package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.JOSEException;
import id.walt.credentials.presentations.formats.VerifiablePresentation;
import id.walt.policies2.vp.policies.VPPolicy2;
import id.walt.policies2.vp.policies.VPPolicy2.PolicyRunResult;
import id.walt.policies2.vp.policies.VPPolicyList;
import no.idporten.eudiw.verifier.ErrorCode;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.walt.WaltPresentations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Verifies a single presentation from a vp_token with walt.id: a parser for the format, and the walt.id VP policies
 * that verify the credential and holder binding. The policies are run with the session data from the authorization
 * request. Trust list and status list are checked by the caller.
 */
public abstract class CredentialVerifier {

    private static final Logger log = LoggerFactory.getLogger(CredentialVerifier.class);

    static final String HOLDER_BINDING_FAILED = "Invalid vp_token. Holder binding failed.";

    private final Clock clock;

    protected CredentialVerifier(Clock clock) {
        this.clock = clock;
    }

    public abstract CredentialVerificationResult verify(String vpToken, DcqlCredentialQuery credentialQuery, VerificationTransaction transaction);

    protected Map<String, PolicyRunResult> runPolicies(VerifiablePresentation presentation, VPPolicyList policies,
                                                       String vpToken, VerificationTransaction transaction) {
        return WaltPresentations.verify(presentation, policies, WaltPresentations.sessionContext(
                vpToken, transaction.getNonce(), transaction.getAudience(), responseUri(transaction),
                encryptionKeyThumbprint(transaction), clock.instant()));
    }

    /** VALID when every selected policy succeeded. */
    protected static ValidationStatus status(Map<String, PolicyRunResult> results, Predicate<String> selected) {
        ValidationStatus status = ValidationStatus.VALID;
        for (Map.Entry<String, PolicyRunResult> result : results.entrySet()) {
            if (selected.test(result.getKey()) && !result.getValue().getSuccess()) {
                log.warn("Policy {} failed: {}", result.getKey(), result.getValue().getErrors());
                status = ValidationStatus.INVALID;
            }
        }
        return status;
    }

    protected static <P extends VPPolicy2> List<P> without(List<P> policies, Predicate<String> excluded) {
        return policies.stream().filter(policy -> !excluded.test(policy.getId())).toList();
    }

    protected static VerificationException holderBindingFailed() {
        return new VerificationException(ErrorCode.INVALID_REQUEST, HOLDER_BINDING_FAILED);
    }

    protected static <T> T parse(Supplier<T> parser, String errorDescription) {
        try {
            return parser.get();
        } catch (VerificationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, errorDescription, e);
        }
    }

    // response_uri as sent in the authorization request
    private static String responseUri(VerificationTransaction transaction) {
        return transaction.getRequest() != null && transaction.getRequest().get("response_uri") instanceof String responseUri
                ? responseUri
                : null;
    }

    // SHA-256 JWK thumbprint (RFC 7638) of the public response encryption key, base64url encoded
    private static String encryptionKeyThumbprint(VerificationTransaction transaction) {
        if (transaction.getEncryptionKey() == null) {
            return null;
        }
        try {
            return transaction.getEncryptionKey().toPublicJWK().computeThumbprint().toString();
        } catch (JOSEException e) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Unable to compute thumbprint of the response encryption key", e);
        }
    }
}
