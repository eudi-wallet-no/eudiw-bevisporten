package no.idporten.sdk.oidcserver.protocol;

public interface AuthenticatedRequest {

    String getAuthorizationHeader();
    String getClientId();
    String getClientSecret();
    String getClientAssertion();
    String getClientAssertionType();
    void setAuthenticatedClientId(String clientId);

    default boolean isAuthenticatedRequest() {
        return isClientSecretPost() || isClientSecretBasic() || isClientSecretJwt();
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

    default boolean hasMoreThanOneClientAuthMethod() {
        return isClientSecretPost() ? isClientSecretBasic() || isClientSecretJwt() : isClientSecretBasic() && isClientSecretJwt();
    }

    void clearAuthentication();

}
