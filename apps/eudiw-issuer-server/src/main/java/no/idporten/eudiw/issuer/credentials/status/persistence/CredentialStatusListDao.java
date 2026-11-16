package no.idporten.eudiw.issuer.credentials.status.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Status list references allocated by us for issued credentials.
 */
@Repository
public class CredentialStatusListDao extends StatusListDao {

    public CredentialStatusListDao(JdbcTemplate jdbc) {
        super(jdbc, "credential_status_list");
    }

}
