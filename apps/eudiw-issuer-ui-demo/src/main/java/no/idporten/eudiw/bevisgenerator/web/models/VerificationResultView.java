package no.idporten.eudiw.bevisgenerator.web.models;

import java.util.List;


public record VerificationResultView(
        String credentialType,
        String displayName,
        boolean valid,
        List<ClaimView> claims,
        List<ValidationDetailView> validationDetails
) {
}
