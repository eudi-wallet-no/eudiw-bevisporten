package no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProofType(
        @JsonProperty("proof_signing_alg_values_supported")
        List<String> proofSigningAlgValuesSupported
) {}
