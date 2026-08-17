package no.idporten.eudiw.byob.service.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "spring.data.redis")
public record RedisProperties(String host, Integer port, String password, Sentinel sentinel) {


    public boolean isSentinal() {
        return sentinel != null && sentinel.nodes() != null && !sentinel.nodes().isEmpty();
    }

    @Valid
    public record Sentinel(@NotEmpty String master, @NotEmpty String password, @NotEmpty List<String> nodes) {}

}
