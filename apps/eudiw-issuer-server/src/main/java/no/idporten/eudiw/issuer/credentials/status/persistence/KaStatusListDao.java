package no.idporten.eudiw.issuer.credentials.status.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Status list references from key attestations (key_storage_status).
 */
@Repository
public class KaStatusListDao extends StatusListDao {

    public KaStatusListDao(JdbcTemplate jdbc) {
        super(jdbc, "ka_status_list");
    }

}
