package no.idporten.eudiw.bevisgenerator.integration.byobservice.model;

import java.util.List;

public record CredentialMetadata(
        List<Display> display,
        List<Claim> claims
) {}
