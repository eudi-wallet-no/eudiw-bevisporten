package no.idporten.eudiw.oauth2.server.protocol;

import java.util.stream.Stream;

public interface AuthenticatedRequest {

    String getAuthorizationHeader();
    String getClientId();
    String getClientSecret();
    String getClientAssertion();
    String getClientAssertionType();
    String getClientAttestation();
    String getClientAttestationPoP();

    void setAuthenticatedClientId(String clientId);

    default boolean isAuthenticatedRequest() {
        return isClientSecretPost() || isClientSecretBasic() || isClientSecretJwt() || isAttestationBased() || isNone();
    }

    default boolean isClientSecretPost() {
        return getClientId() != null && getClientSecret() != null;
    }

    default boolean isClientSecretBasic() {
        return getAuthorizationHeader() != null && getAuthorizationHeader().startsWith("Basic ");
    }

    default boolean isClientSecretJwt() {
        return "urn:ietf:params:oauth:client-assertion-type:jwt-bearer".equals(getClientAssertionType());
    }

    default boolean isAttestationBased() {
        return getClientAttestation() != null && getClientAttestationPoP() != null;
    }

    default boolean isNone() {
        return getClientId() != null && !(isClientSecretBasic() || isClientSecretJwt() || isClientSecretPost() || isAttestationBased());
    }

    default boolean hasMoreThanOneClientAuthMethod() {
        return Stream.of(isClientSecretPost(), isClientSecretBasic(), isClientSecretJwt(), isAttestationBased())
                .filter(Boolean::booleanValue)
                .count() > 1;
    }

    void clearAuthentication();

}
