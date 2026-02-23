package no.idporten.eudiw.byob.service.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Context for filtering credential configurations based on access level and allowed prefix.
 *
 * @param accessAll may access all with allowed prefix
 * @param allowedPrefix prefix for filtering
 */
public record CredentialConfigurationContext(boolean accessAll, String allowedPrefix) {

    public static final String PUBLIC_CREDENTIAL_TYPE_PREFIX = "net.eidas2sandkasse:";

    /**
     * Context for admin endpoints.  Access all, do anything with everything.
     */
    public static CredentialConfigurationContext forAdmin() {
        return new CredentialConfigurationContext(true, "");
    }

    /**
     * Context for public endpoints when editing.  Access only public credential types, do anything with these.
     */
    public static CredentialConfigurationContext forPublic() {
        return new CredentialConfigurationContext(false, PUBLIC_CREDENTIAL_TYPE_PREFIX);
    }

    /**
     * Create a context from the request.
     *
     * All operations in the admin API gets admin.
     * All operations in the public API gets public.
     */
    public static CredentialConfigurationContext fromRequest(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/v1/admin")) {
            return forAdmin();
        }
        return forPublic();
    }

    /**
     * Does this context allow access to credential type?
     */
    public boolean hasAccessToCredentialType(String credentialType) {
        return accessAll || (credentialType != null && credentialType.startsWith(allowedPrefix));
    }

}
