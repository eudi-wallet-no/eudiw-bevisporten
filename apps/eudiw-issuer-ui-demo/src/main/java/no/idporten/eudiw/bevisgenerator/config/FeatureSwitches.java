package no.idporten.eudiw.bevisgenerator.config;

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
     * Allow version 2 of bevistype (BYOB).
     */
    private boolean allowBevistyperV2 = false;

    public boolean isAllowBevistyperV2() {
        return allowBevistyperV2;
    }

    public void setAllowBevistyperV2(boolean allowBevistyperV2) {
        this.allowBevistyperV2 = allowBevistyperV2;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Will set allow-bevistyper-v2 to {}", isAllowBevistyperV2());
    }
}
