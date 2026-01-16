package no.idporten.eudiw.issuer.ui.demo.byob.model;

import java.util.List;

public record CredentialMetadata(
        List<Display> display,
        List<Claim> claims
) {}
