package no.idporten.eudiw.verifier.statuslist;

import net.eidas2sandkasse.tsl.TokenStatusListValidator;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;

/**
 * Responsible for checking revocation status. Calls Status list API with
 * status list specified inside the proof, gets the list and checks the
 * status on the index that is inside the proof.
 *
 */

@Service
public class TokenStatuslistService {
    private final TokenStatusListValidator tokenStatusListValidator;

    private static final Logger log = LoggerFactory.getLogger(TokenStatuslistService.class);

    public TokenStatuslistService(TokenStatusListValidator tokenStatusListValidator) {
        this.tokenStatusListValidator = tokenStatusListValidator;    }

    public ValidationStatus lookupStatusFromTokenStatuslist(URI uri, int idx) {
        try {
            boolean valid = tokenStatusListValidator.validate(uri, idx);
            return valid ? ValidationStatus.VALID : ValidationStatus.INVALID;
        } catch (Exception e) {
            log.error("Set validation-status=INCONCLUSIVE since error in communication with status-api: {}", uri, e);
            return ValidationStatus.INCONCLUSIVE;
        }
    }

    public @NonNull String getValidationDetail(ValidationStatus status) {
        return switch (status) {
            case INCONCLUSIVE -> "Statusliste: validering feila";
            case VALID -> "Statusliste: bevis er gyldig";
            case INVALID -> "Statusliste: bevis er revokert";
            case NOT_APPLICABLE -> "Statusliste: bevis er ikke revokerbart";
            default -> "Statusliste: ukjent status";
        };
    }

}
