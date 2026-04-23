package no.idporten.eudiw.issuer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Collection;
import java.util.List;

/**
 * Schedule continuous updates of credential configurations from HTTP sources.
 */
@Configuration
public class CredentialConfigurationSourceScheduler implements InitializingBean {

    Logger log = LoggerFactory.getLogger(CredentialConfigurationSourceScheduler.class);

    private final TaskScheduler taskScheduler;
    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    public CredentialConfigurationSourceScheduler(CredentialIssuerServerProperties credentialIssuerServerProperties, TaskScheduler taskScheduler) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<CredentialConfigurationSource> configurationSources = credentialIssuerServerProperties.getTenants().values().stream().map(CredentialIssuerTenant::getCredentialConfigurationSources).flatMap(Collection::stream).toList();
        for (CredentialConfigurationSource credentialConfigurationSource : configurationSources) {
            if (credentialConfigurationSource instanceof HttpCredentialConfigurationSource) {
                scheduleUpdate("*/30 * * * * *", credentialConfigurationSource);
            }
        }
    }

    private void scheduleUpdate(String cronExpression, CredentialConfigurationSource credentialConfigurationSource) {
        Runnable task = () -> {
            log.info("Executing scheduled task to update credential configurations from {}", credentialConfigurationSource.getProperties().api().uri());
            credentialConfigurationSource.refresh();
        };
        CronTrigger cronTrigger = new CronTrigger(cronExpression);
        taskScheduler.schedule(task, cronTrigger);
    }

}
