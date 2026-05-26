package no.idporten.eudiw.statuslist.repository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("status-list.list-config")
public record StatusListProperties(@Min(1) int listSize, @Min(1) @Max(8) int bitsPerStatus) {
}
