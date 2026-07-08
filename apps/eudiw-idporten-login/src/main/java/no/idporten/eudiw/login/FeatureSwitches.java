package no.idporten.eudiw.login;

import lombok.Data;
import no.idporten.validators.identifier.PersonIdentifierValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "eudiw-idporten-login.features")
public class FeatureSwitches implements InitializingBean {

    private boolean allowRealPersonIdentifiers = true;
    private boolean allowSyntheticPersonIdentifiers = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        PersonIdentifierValidator.setRealPersonIdentifiersAllowed(allowRealPersonIdentifiers);
        PersonIdentifierValidator.setSyntheticPersonIdentifiersAllowed(allowSyntheticPersonIdentifiers);
    }

}
