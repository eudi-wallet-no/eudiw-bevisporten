package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import java.util.List;
import java.util.Map;

public record CredentialDefinitionDisplayData(
        String id,
        String title,
        String issuer,
        String format,
        Map<String, Object> meta,
        List<SelectableClaim> claims
) { }
