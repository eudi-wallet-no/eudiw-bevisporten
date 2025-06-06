package no.idporten.sdk.oidcserver.protocol;

/**
 * Interface for exposing info for audit logging.
 */
public interface AuditDataProvider {

    AuditData getAuditData();

}
