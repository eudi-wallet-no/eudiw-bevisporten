package no.idporten.eudiw.verifier.config;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.verifier.VerificationException;
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
    @NotEmpty
    private String siop2ClientId;
    @NotEmpty
    private Map<String, @Valid ClientApplication> clientApplications;


    public ClientApplication findClientApplication(@NotEmpty String clientApplicationId) {
        if(clientApplications.containsKey(clientApplicationId)) {
            return clientApplications.get(clientApplicationId);
        } else {
            throw new VerificationException("invalid_request", "Unknown client application");
        }
    }

    @Override
    public void afterPropertiesSet() {
        for(String id : clientApplications.keySet()) {
            clientApplications.get(id).setId(id);
        }
    }
}
