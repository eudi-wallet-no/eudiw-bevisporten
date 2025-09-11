package no.idporten.eudiw.issuer.claimssource.skatteetaten;

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
