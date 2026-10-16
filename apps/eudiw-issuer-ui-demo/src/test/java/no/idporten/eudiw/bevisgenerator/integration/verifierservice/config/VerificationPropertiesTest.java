package no.idporten.eudiw.bevisgenerator.integration.verifierservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("When binding verifier configuration, then the configured client application ID is expected")
class VerificationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("VERIFIER_SERVICE_API_KEY=junit-api-key");

    @Test
    @DisplayName("When application.yaml is loaded, then the existing client application ID is expected")
    void loadsDefaultClientApplicationId() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(VerificationProperties.class).clientApplicationId())
                    .isEqualTo("bevisgenerator-login");
        });
    }

    @Test
    @DisplayName("When the environment placeholder is set, then its client application ID is expected")
    void resolvesClientApplicationIdPlaceholder() {
        contextRunner.withPropertyValues("VERIFIER_SERVICE_CLIENT_APPLICATION_ID=environment-login")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(VerificationProperties.class).clientApplicationId())
                            .isEqualTo("environment-login");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(VerificationProperties.class)
    static class TestConfiguration {
    }
}
