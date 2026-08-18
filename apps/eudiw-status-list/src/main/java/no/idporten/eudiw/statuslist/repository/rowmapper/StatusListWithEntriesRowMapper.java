package no.idporten.eudiw.statuslist.repository.rowmapper;

import no.idporten.eudiw.statuslist.repository.models.StatusListWithEntriesDto;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class StatusListWithEntriesRowMapper implements ResultSetExtractor<StatusListWithEntriesDto> {
    private final int bitsPerStatus;

    public StatusListWithEntriesRowMapper(int bitsPerStatus) {
        this.bitsPerStatus = bitsPerStatus;
    }

    @Override
    public StatusListWithEntriesDto extractData(ResultSet rs) throws SQLException {
        StatusListWithEntriesDto sl = null;
        while (rs.next()) {
            if (sl == null) {
                sl = new StatusListWithEntriesDto(
                        rs.getInt("id"),
                        rs.getInt("list_size"),
                        bitsPerStatus,
                        new HashMap<>()
                );
            }

            Integer listIndex = rs.getObject("list_index", Integer.class);
            Integer statusValue = rs.getObject("status_value", Integer.class);
            if (listIndex != null && statusValue != null) {
                sl.entries().put(listIndex, statusValue);
            }
        }

        return sl;
    }
}
