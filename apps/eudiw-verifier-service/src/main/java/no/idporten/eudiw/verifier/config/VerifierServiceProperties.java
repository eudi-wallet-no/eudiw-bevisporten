package no.idporten.eudiw.verifier.config;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.verifier.VerificationException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

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
    @NotEmpty
    private String siop2ClientId;
    @NotEmpty
    private Map<String, @Valid ClientApplication> clientApplications;


    public ClientApplication findClientApplication(@NotEmpty String clientApplicationId) {
        ClientApplication clientApplication = clientApplications.get(clientApplicationId);

        if (clientApplication == null || clientApplication.isDisabled()) {
            throw new VerificationException("invalid_request", "Unknown client application");
        }

        return clientApplication;
    }

    public ClientApplication validateClientApplication(@NotEmpty String clientApplicationId, String apiKey) {
        ClientApplication clientApplication = clientApplications.get(clientApplicationId);

        if (clientApplication == null || clientApplication.isDisabled()) {
            throw new VerificationException("invalid_request", "Unknown client application");
        }

        if (apiKey == null || !apiKey.equals(clientApplication.getApiKey())) {
            // TODO: replace logging with exception when clients are ready
            LoggerFactory.getLogger(VerifierServiceProperties.class).warn("Invalid API Key for client application: " + clientApplicationId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key"); // TODO skrive om til custom exception som håndteres i ControllerAdvice
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
