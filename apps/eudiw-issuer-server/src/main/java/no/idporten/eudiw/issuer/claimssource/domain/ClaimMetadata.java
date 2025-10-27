package no.idporten.eudiw.issuer.claimssource.domain;

import java.util.Map;

public record ClaimMetadata (
        String name,
        // locale -> text
        Map<String, String> displayNames,
        boolean mandatory,
        String validationRegex
)
{
}
