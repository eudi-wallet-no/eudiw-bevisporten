package no.idporten.eudiw.statuslist.repository.rowmapper;

import no.idporten.eudiw.statuslist.repository.StatusListProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RowMapperConfiguration {
    private final StatusListProperties statusListProperties;

    public RowMapperConfiguration(StatusListProperties statusListProperties) {
        this.statusListProperties = statusListProperties;
    }


    @Bean
    public StatusListRowMapper statusListRowMapper() {
        return new StatusListRowMapper();
    }

    @Bean
    public StatusListWithEntriesRowMapper statusListWithEntriesRowMapper() {
        return new StatusListWithEntriesRowMapper(statusListProperties.bitsPerStatus());
    }
}
