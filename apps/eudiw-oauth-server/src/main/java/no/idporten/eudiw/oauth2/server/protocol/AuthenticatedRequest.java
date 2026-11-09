package no.idporten.eudiw.oauth2.server.protocol;

public interface AuthenticatedRequest {

    /**
     * The client_id request parameter.  Use {@link #getClientAuthentication()} for the authenticated client.
     */
    String getClientId();
    String getClientAttestation();
    String getClientAttestationPoP();

    /**
     * The result of a successful client authentication, kept after {@link #clearAuthentication()}.
     */
    ClientAuthentication getClientAuthentication();

    void setClientAuthentication(ClientAuthentication clientAuthentication);

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
