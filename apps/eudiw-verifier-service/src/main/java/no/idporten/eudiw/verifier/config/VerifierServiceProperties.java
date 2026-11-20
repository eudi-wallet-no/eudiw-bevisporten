package no.idporten.eudiw.verifier.config;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.eidas2sandkasse.tsl.TokenStatusListValidatorConfig;
import no.idporten.eudiw.verifier.InvalidClientException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
@Component
@ConfigurationProperties(prefix = "verifier-service")
public class VerifierServiceProperties implements InitializingBean {

    @NotEmpty
    private String externalBaseUri;
    private String siop2ClientId = "";
    @NotEmpty
    private Map<String, @Valid ClientApplication> clientApplications;

    @NotNull
    private TokenStatusListValidatorConfig tokenStatusListValidatorConfig;

    public ClientApplication findClientApplication(@NotEmpty String clientApplicationId) {
        ClientApplication clientApplication = clientApplications.get(clientApplicationId);

        if (clientApplication == null || clientApplication.isDisabled()) {
            throw new InvalidClientException("Unknown client application", "Unknown client application %s".formatted(clientApplicationId));
        }

        return clientApplication;
    }

    public ClientApplication validateClientApplication(@NotEmpty String clientApplicationId, String apiKey) {
        ClientApplication clientApplication = clientApplications.get(clientApplicationId);

        if (clientApplication == null || clientApplication.isDisabled()) {
            throw new InvalidClientException("Unknown client application", "Unknown client application %s".formatted(clientApplicationId));
        }


        if (apiKey == null || !apiKey.equals(clientApplication.getApiKey())) {
            throw new InvalidClientException(
                    "Invalid API Key",
                    "Invalid API key for client application %s".formatted(clientApplicationId)
            );
        }

        return clientApplication;
    }

    @Override
    public void afterPropertiesSet() {
        for (String id : clientApplications.keySet()) {
            clientApplications.get(id).setId(id);
        }
    }
}
