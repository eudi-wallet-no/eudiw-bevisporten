package no.idporten.eudiw.oauth2.server.protocol;

/**
 * Interface for exposing info for audit logging.
 */
public interface AuditDataProvider {

    AuditData getAuditData();

}
