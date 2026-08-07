package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import java.util.List;

public record SelectableClaim(
        String label,
        List<String> path
) { }
