package no.idporten.eudiw.issuer.ui.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "bevisgenerator.features")
public class FeatureSwitches implements InitializingBean {

    private final static Logger log = LoggerFactory.getLogger(FeatureSwitches.class);

    /**
     * Allow creation of bevistype (BYOB).
     */
    private boolean allowCreateBevistyper = false;

    public boolean isAllowCreateBevistyper() {
        return allowCreateBevistyper;
    }

    public void setAllowCreateBevistyper(boolean allowCreateBevistyper) {
        this.allowCreateBevistyper = allowCreateBevistyper;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Will set allow-create-bevistyper to {}", isAllowCreateBevistyper());
    }
}
