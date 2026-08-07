package no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.Display;

import java.util.List;

public record CredentialConfigurationMetadata(
        List<Display> display,
        List<ClaimMetadata> claims
) {}
