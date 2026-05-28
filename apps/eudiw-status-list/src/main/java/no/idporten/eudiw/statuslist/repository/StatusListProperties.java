package no.idporten.eudiw.statuslist.repository;

import jakarta.validation.constraints.Min;
import no.idporten.eudiw.statuslist.validation.ValidBitsPerStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("status-list.list-config")
public record StatusListProperties(@Min(1) int listSize, @ValidBitsPerStatus int bitsPerStatus) {
}
