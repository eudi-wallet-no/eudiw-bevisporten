package no.idporten.sdk.oidcserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import no.idporten.sdk.oidcserver.util.JsonUtils;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Builder
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OpenIDProviderMetadataResponse implements JsonResponse {

    public static final String ISSUER = "issuer";
    public static final String PUSHED_AUTHORIZATION_REQUEST_ENDPOINT = "pushed_authorization_request_endpoint";
    public static final String REQUIRE_PUSHED_AUTHORIZATION_REQUESTS = "require_pushed_authorization_requests";
    public static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    public static final String TOKEN_ENDPOINT = "token_endpoint";
    public static final String JWKS_URI = "jwks_uri";
    public static final String USERINFO_ENDPOINT = "userinfo_endpoint";
    public static final String SCOPES_SUPPORTED = "scopes_supported";
    public static final String CLAIMS_SUPPORTED = "claims_supported";
    public static final String AUTHORIZATION_DETAILS_TYPES_SUPPORTED = "authorization_details_types_supported";
    public static final String RESPONSE_TYPES_SUPPORTED = "response_types_supported";
    public static final String RESPONSE_MODES_SUPPORTED = "response_modes_supported";
    public static final String GRANT_TYPES_SUPPORTED = "grant_types_supported";
    public static final String ACR_VALUES_SUPPORTED = "acr_values_supported";
    public static final String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";
    public static final String CODE_CHALLENGE_METHODS_SUPPORTED = "code_challenge_methods_supported";
    public static final String UI_LOCALES_SUPPORTED = "ui_locales_supported";
    public static final String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "token_endpoint_auth_methods_supported";
    public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED = "token_endpoint_auth_signing_alg_values_supported";
    public static final String AUTHORIZATION_SIGNING_ALG_VALUES_SUPPORTED = "authorization_signing_alg_values_supported";
    public static final String ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported";
    public static final String AUTHORIZATION_RESPONSE_ISS_PARAMETER_SUPPORTED = "authorization_response_iss_parameter_supported";

    @JsonProperty(ISSUER)
    private URI issuer;

    @JsonProperty(PUSHED_AUTHORIZATION_REQUEST_ENDPOINT)
    private URI pushedAuthorizationRequestEndpoint;

    @JsonProperty(REQUIRE_PUSHED_AUTHORIZATION_REQUESTS)
    @Builder.Default
    private boolean requirePushedAuthorizationRequests = true;

    @JsonProperty(AUTHORIZATION_ENDPOINT)
    private URI authorizationEndpoint;

    @JsonProperty(TOKEN_ENDPOINT)
    private URI tokenEndpoint;

    @JsonProperty(JWKS_URI)
    private URI jwksUri;

    @JsonProperty(USERINFO_ENDPOINT)
    private URI userinfoEndpoint;

    @JsonProperty(SCOPES_SUPPORTED)
    @Singular("scopeSupported")
    private Set<String> scopesSupported;

    @JsonProperty(CLAIMS_SUPPORTED)
    @Singular("claimSupported")
    private Set<String> claimsSupported;

    @JsonProperty(AUTHORIZATION_DETAILS_TYPES_SUPPORTED)
    @Singular("authorizationDetailsTypeSupported")
    private Set<String> authorizationDetailsTypesSupported;

    @JsonProperty(RESPONSE_TYPES_SUPPORTED)
    @Builder.Default
    private List<String> responseTypesSupported = List.of("code");

    @JsonProperty(RESPONSE_MODES_SUPPORTED)
    @Singular("responseModeSupported")
    private Set<String> responseModesSupported;

    @JsonProperty(GRANT_TYPES_SUPPORTED)
    @Builder.Default
    private List<String> grantTypesSupported = List.of("authorization_code");

    @JsonProperty(ACR_VALUES_SUPPORTED)
    @Singular("acrValueSupported")
    private List<String> acrValuesSupported;

    @JsonProperty(SUBJECT_TYPES_SUPPORTED)
    @Builder.Default
    private List<String> subjectTypesSupported = List.of("public");

    @JsonProperty(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED)
    @Singular("idTokenSigningAlgValueSupported")
    private List<String> idTokenSigningAlgValuesSupported;

    @JsonProperty(AUTHORIZATION_SIGNING_ALG_VALUES_SUPPORTED)
    @Singular("authorizationSigningAlgValueSupported")
    private List<String> authorizationSigningAlgValuesSupported;

    @JsonProperty(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED)
    @Builder.Default
    private List<String> tokenEndpointAuthMethodsSupported = List.of("client_secret_basic", "client_secret_post", "client_secret_jwt", "none");

    @JsonProperty(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED)
    @Singular("tokenEndpointAuthSigningAlgValueSupported")
    private List<String> tokenEndpointAuthSigningAlgValuesSupported;

    @JsonProperty(UI_LOCALES_SUPPORTED)
    @Singular("uiLocaleSupported")
    private List<String> uiLocalesSupported;

    @JsonProperty(CODE_CHALLENGE_METHODS_SUPPORTED)
    @Singular("codeChallengeMethodSupported")
    private List<String> codeChallengeMethodsSupported;

    @JsonProperty(AUTHORIZATION_RESPONSE_ISS_PARAMETER_SUPPORTED)
    @Builder.Default
    private boolean authorizationResponseIssParameterSupported = true;

    @Override
    public Map<String, Object> toJsonObject() {
        return JsonUtils.jsonObjectBuilder()
                .addAttribute(ISSUER, issuer)
                .addAttribute(PUSHED_AUTHORIZATION_REQUEST_ENDPOINT, pushedAuthorizationRequestEndpoint)
                .addAttribute(REQUIRE_PUSHED_AUTHORIZATION_REQUESTS, requirePushedAuthorizationRequests)
                .addAttribute(AUTHORIZATION_ENDPOINT, authorizationEndpoint)
                .addAttribute(TOKEN_ENDPOINT, tokenEndpoint)
                .addAttribute(JWKS_URI, jwksUri)
                .addAttribute(USERINFO_ENDPOINT, userinfoEndpoint)
                .addAttribute(SCOPES_SUPPORTED, scopesSupported)
                .addAttribute(CLAIMS_SUPPORTED, claimsSupported)
                .addAttribute(AUTHORIZATION_DETAILS_TYPES_SUPPORTED, authorizationDetailsTypesSupported)
                .addAttribute(RESPONSE_TYPES_SUPPORTED, responseTypesSupported)
                .addAttribute(RESPONSE_MODES_SUPPORTED, responseModesSupported)
                .addAttribute(GRANT_TYPES_SUPPORTED, grantTypesSupported)
                .addAttribute(ACR_VALUES_SUPPORTED, acrValuesSupported)
                .addAttribute(SUBJECT_TYPES_SUPPORTED, subjectTypesSupported)
                .addAttribute(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, idTokenSigningAlgValuesSupported)
                .addAttribute(AUTHORIZATION_SIGNING_ALG_VALUES_SUPPORTED, authorizationSigningAlgValuesSupported)
                .addAttribute(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED, tokenEndpointAuthMethodsSupported)
                .addAttribute(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED, tokenEndpointAuthSigningAlgValuesSupported)
                .addAttribute(UI_LOCALES_SUPPORTED, uiLocalesSupported)
                .addAttribute(CODE_CHALLENGE_METHODS_SUPPORTED, codeChallengeMethodsSupported)
                .addAttribute(AUTHORIZATION_RESPONSE_ISS_PARAMETER_SUPPORTED, authorizationResponseIssParameterSupported)
                .build();
    }

}
