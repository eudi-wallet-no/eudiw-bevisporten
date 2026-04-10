package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;

public record AuthoritativeSourceProperties(@NotNull URI uri,
                                            @DefaultValue("3s") Duration connectTimeout,
                                            @DefaultValue("3s") Duration readTimeout,
                                            String apiKeyHeader,
                                            String apiKey)
{

    public boolean useApiKey() {
        return StringUtils.hasText(apiKeyHeader) && StringUtils.hasText(apiKey);
    }

}
