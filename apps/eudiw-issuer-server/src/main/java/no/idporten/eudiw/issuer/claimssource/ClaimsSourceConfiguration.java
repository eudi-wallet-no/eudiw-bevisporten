package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import no.digdir.freg.service.FregService;
import no.idporten.eudiw.issuer.claimssource.pid.PIDClaimsSource;
import no.idporten.eudiw.issuer.claimssource.pid.PersonConverterService;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

@RequiredArgsConstructor
@Configuration
public class ClaimsSourceConfiguration implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final GenericApplicationContext applicationContext;

    Logger logger = LoggerFactory.getLogger(ClaimsSourceConfiguration.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        for (ClaimsSourceProperties claimsSourceProperties : credentialIssuerServerProperties.getClaimsSources()) {
            applicationContext.registerBean(claimsSourceProperties.getDoctype() + "-ClaimsSource", ClaimsSource.class, () -> {
                ClaimsSource claimsSource;
                try {
                    Class<?> clazz = Class.forName(claimsSourceProperties.getClassName());
                    logger.info("Creating PIDClaimsSource with FregPIDService"+claimsSourceProperties.getClassName()+",   "+PIDClaimsSource.class.getName());
                    if(claimsSourceProperties.getClassName().equals(PIDClaimsSource.class.getName())) {
                        logger.info("Creating PIDClaimsSource with FregPIDService");
                        // hack for now to inject fregService and personConverterService
                        FregService fregService = applicationContext.getBean(FregService.class);
                        PersonConverterService personConverterService = applicationContext.getBean(PersonConverterService.class);
                        claimsSource = (ClaimsSource) clazz.getConstructor(FregService.class, PersonConverterService.class).newInstance(fregService, personConverterService);
                    }else {
                        logger.info("Creating non pid ClaimsSource");
                        claimsSource = (ClaimsSource) clazz.getConstructor().newInstance();
                    }
                    claimsSource.init(claimsSourceProperties);
                    return claimsSource;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
