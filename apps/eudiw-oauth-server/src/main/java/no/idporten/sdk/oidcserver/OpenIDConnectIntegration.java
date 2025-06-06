package no.idporten.sdk.oidcserver;

import com.nimbusds.jose.jwk.JWKSet;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.*;

/**
 * An SDK for a subset of the OpenID Connect protocol.  Implement this interface or extend the base implementation in
 * {@link OpenIDConnectIntegrationBase} to create OPenID Connect provider functionality for the authorization code
 * flow with pushed authorization requests and signed id_tokens.
 */
public interface OpenIDConnectIntegration {

    /**
     * Process a pushed authorization request and generate a response.  This method can be used in the implementation of
     * the pushed authorization request endpoint.  The response contains the PAR request_uri to be used by the client
     * with the authorization endpoint.
     *
     * @param authorizationRequest
     * @return authorization response
     * @throws OAuth2Exception if request processing fails
     */
    PushedAuthorizationResponse process(PushedAuthorizationRequest authorizationRequest) throws OAuth2Exception;

    /**
     * Validates a pushed authorization request after client authentication.
     *
     * @param authorizationRequest
     * @throws OAuth2Exception if request validation fails
     */
    void validate(PushedAuthorizationRequest authorizationRequest, ClientMetadata clientMetadata) throws OAuth2Exception;

    /**
     * Authenticates client with information from request.
     *
     * @throws OAuth2Exception if client authentication fails
     */
    ClientMetadata authenticateClient(AuthenticatedRequest authenticatedRequest) throws OAuth2Exception;

    /**
     * Retrieves a pushed authorization request by it's request_uri, represented in an authorization request.  This
     * method can be used in the implementation of the authorization endpoint.
     *
     * @param authorizationRequest authorization request
     * @return pushed authorization request
     */
    PushedAuthorizationRequest process(AuthorizationRequest authorizationRequest) throws OAuth2Exception;

    /**
     * Validates an authorization request.
     *
     * @param authorizationRequest
     * @throws OAuth2Exception if request validation fails
     */
    void validate(AuthorizationRequest authorizationRequest) throws OAuth2Exception;

    /**
     * Creates an authorization after authentication the end user.  This method can be used when the application
     * wants to give control back to the client application.  The authorization response contains the authorization
     * code and other information to create a response to the client application.
     *
     * @param pushedAuthorizationRequest the client's authorization request
     * @param authorization              authorization with information about the end user and the authentication process
     * @return authorization response
     */
    AuthorizationResponse authorize(PushedAuthorizationRequest pushedAuthorizationRequest, Authorization authorization);

    /**
     * Creates an authorization response with an error parameter.  This method can be used when the application wants
     * to give control back to the client application, without authenticating the user.
     *
     * @param pushedAuthorizationRequest the client's authorization request
     * @param error                      error code
     * @param errorDescription           error description
     * @return authorization error response
     */
    AuthorizationResponse errorResponse(PushedAuthorizationRequest pushedAuthorizationRequest, String error, String errorDescription);

    /**
     * Creates an authorization error response to a client.  This method can be used when the application has lost
     * information about the original authorization request.  The response is a redirected JARM response to the first
     * redirect_url in the client's metadata.  As the state parameter has been lost, the client application must be aware
     * of such out-of-context responses form this issuer.  The client must validate the signature and the expiration of
     * the authorization error response.
     *
     * @param clientMetadata client metadata
     * @param error error code
     * @param errorDescription error description
     * @return authorization error response
     */
    AuthorizationResponse panicErrorResponse(ClientMetadata clientMetadata, String error, String errorDescription);

    /**
     * Processes a token request and creates a token response, containing an id_token.
     *
     * @param tokenRequest token request
     * @return token response
     * @throws OAuth2Exception
     */
    TokenResponse process(TokenRequest tokenRequest) throws OAuth2Exception;

    /**
     * Validates a token request after client authentication.
     *
     * @param tokenRequest
     * @throws OAuth2Exception if request validation fails
     */
    void validate(TokenRequest tokenRequest, ClientMetadata clientMetadata) throws OAuth2Exception;

    /**
     * Gets the public set of Json Web Keys.  Can be used to create the jwks endpoint.
     *
     * @return
     */
    JWKSet getPublicJWKSet();

    /**
     * Gets the OpenID provider Metadata.  Can be used to create a discovery endpoint.
     *
     * @return
     */
    OpenIDProviderMetadataResponse getOpenIDProviderMetadata();

    /**
     * Processes a userinfo requests and creates a userinfo response.
     *
     * @param userInfoRequest userinfo request object
     * @return userinfo response
     * @throws OAuth2Exception if request validation fails or access_token is invalid
     */
    UserInfoResponse process(UserInfoRequest userInfoRequest);

    /**
     * Validates a UserInfo request.
     *
     * @param userInfoRequest
     * @throws OAuth2Exception if request validation fails
     */
    void validate(UserInfoRequest userInfoRequest);

    /**
     * Utility method creating responses for the client applications.  Handles plain and signed responses for
     * query and form_post.
     *
     * @param authorizationResponse authorization response
     */
    ClientResponse createClientResponse(AuthorizationResponse authorizationResponse);

    /**
     * Utility method for finding a client's metadata.
     *
     * @param clientId client id
     * @return client metadata, null if not found
     */
    ClientMetadata findClient(String clientId);

    /**
     * Utility method to access the SDKs configuration.
     *
     * @return SDK configuration
     */
    OpenIDConnectSdkConfiguration getSDKConfiguration();


}