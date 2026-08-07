package no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.Display;

import java.util.List;
import java.util.Map;

public record CredentialIssuerMetadata(
        @JsonProperty("credential_issuer")
        String credentialIssuer,

        @JsonProperty("authorization_servers")
        List<String> authorizationServers,

        @JsonProperty("credential_endpoint")
        String credentialEndpoint,

        @JsonProperty("nonce_endpoint")
        String nonceEndpoint,

        @JsonProperty("notification_endpoint")
        String notificationEndpoint,

        @JsonProperty("credential_configurations_supported")
        Map<String, CredentialConfiguration> credentialConfigurationsSupported,

        List<Display> display
) {}
