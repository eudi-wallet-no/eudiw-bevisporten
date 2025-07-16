package no.idporten.eudiw.oauthserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.idporten.validators.identifier.PersonIdentifierValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@Data
@Slf4j
@Validated
@ConfigurationProperties(prefix = "oauth-authorization-server.features")
public class FeatureSwitches implements InitializingBean {

    private boolean allowRealPersonIdentifiers = true;
    private boolean allowSyntheticPersonIdentifiers = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        PersonIdentifierValidator.setRealPersonIdentifiersAllowed(allowRealPersonIdentifiers);
        PersonIdentifierValidator.setSyntheticPersonIdentifiersAllowed(allowSyntheticPersonIdentifiers);
    }

}
