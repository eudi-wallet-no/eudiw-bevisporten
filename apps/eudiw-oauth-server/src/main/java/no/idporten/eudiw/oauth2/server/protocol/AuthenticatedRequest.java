package no.idporten.eudiw.oauth2.server.protocol;

public interface AuthenticatedRequest {

    String getClientId();
    String getClientAttestation();
    String getClientAttestationPoP();

    void setAuthenticatedClientId(String clientId);

    default boolean isAuthenticatedRequest() {
        return isAttestationBased() || isNone();
    }


    default boolean isAttestationBased() {
        return getClientAttestation() != null && getClientAttestationPoP() != null;
    }

    default boolean isNone() {
        return getClientId() != null && !isAttestationBased();
    }

    default boolean hasMoreThanOneClientAuthMethod() {
        return false; // none and attestation overlaps
    }

    void clearAuthentication();

}
