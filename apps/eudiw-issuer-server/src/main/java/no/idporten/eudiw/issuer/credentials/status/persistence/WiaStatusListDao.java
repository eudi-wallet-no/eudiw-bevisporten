package no.idporten.eudiw.issuer.credentials.status.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Status list references from wallet instance attestations (client_status).
 */
@Repository
public class WiaStatusListDao extends StatusListDao {

    public WiaStatusListDao(JdbcTemplate jdbc) {
        super(jdbc, "wia_status_list");
    }

}
