package no.idporten.eudiw.statuslist.repository.rowmapper;

import no.idporten.eudiw.statuslist.repository.models.StatusListDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StatusListRowMapper implements RowMapper<StatusListDto> {

    @Override
    public StatusListDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StatusListDto(
                rs.getInt("id"),
                rs.getInt("list_size"),
                rs.getInt("seed"),
                rs.getInt("next_index")
        );
    }
}