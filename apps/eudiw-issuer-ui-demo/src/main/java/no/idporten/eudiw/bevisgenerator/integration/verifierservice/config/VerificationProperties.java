package no.idporten.eudiw.bevisgenerator.integration.verifierservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("bevisgenerator.verifier-service")
public record VerificationProperties(
        URI baseUrl,
        String verificationStartEndpoint,
        String verificationStatusEndpoint,
        String verificationResultEndpoint,
        String clientApplicationId) {
}
