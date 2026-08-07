package no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record CredentialConfiguration(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String doctype,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String vct,

        String scope,

        String format,

        @JsonProperty("cryptographic_binding_methods_supported")
        List<String> cryptographicBindingMethodsSupported,

        @JsonProperty("credential_signing_alg_values_supported")
        List<String> credentialSigningAlgValuesSupported,

        @JsonProperty("credential_metadata")
        CredentialConfigurationMetadata credentialMetadata,

        @JsonProperty("proof_types_supported")
        Map<String, ProofType> proofTypesSupported
) {}
