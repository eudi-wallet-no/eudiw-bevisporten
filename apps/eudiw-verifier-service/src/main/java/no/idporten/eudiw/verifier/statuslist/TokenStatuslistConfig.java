package no.idporten.eudiw.verifier.statuslist;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "status-list")
public record TokenStatuslistConfig (
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("60s") Duration clockSkew
){}
