package no.idporten.eudiw.statuslist.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("status-list.provider")
public record StatusProviderProperties(URI uri, Duration valid, Duration ttl) {
}
