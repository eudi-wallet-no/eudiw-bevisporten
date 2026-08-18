package no.idporten.eudiw.login;

import no.idporten.validators.identifier.PersonIdentifierValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "eudiw-idporten-login.features")
public record FeatureSwitches(
        @DefaultValue("true") boolean allowRealPersonIdentifiers,
        @DefaultValue("false") boolean allowSyntheticPersonIdentifiers
) implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        PersonIdentifierValidator.setRealPersonIdentifiersAllowed(allowRealPersonIdentifiers);
        PersonIdentifierValidator.setSyntheticPersonIdentifiersAllowed(allowSyntheticPersonIdentifiers);
    }

}
