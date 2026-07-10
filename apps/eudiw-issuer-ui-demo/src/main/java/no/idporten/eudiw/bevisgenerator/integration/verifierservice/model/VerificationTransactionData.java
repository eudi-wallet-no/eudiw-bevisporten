package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import java.net.URI;

public record VerificationTransactionData(
        VerificationStartResponse verificationStartResponse,
        URI statusUri,
        URI resultUri
) { }
