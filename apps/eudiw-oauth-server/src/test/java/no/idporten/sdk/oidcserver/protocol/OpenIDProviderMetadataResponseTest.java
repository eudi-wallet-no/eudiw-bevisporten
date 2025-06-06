package no.idporten.sdk.oidcserver.protocol;

import no.idporten.sdk.oidcserver.util.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When generating OpenID Connect Server Metadata responses")
public class OpenIDProviderMetadataResponseTest {

    @Test
    @DisplayName("then core SDK default settings are included")
    public void testDefaultMetadata() {
        OpenIDProviderMetadataResponse metadata = OpenIDProviderMetadataResponse.builder().build();
        assertAll(
                () -> assertEquals(1, metadata.getResponseTypesSupported().size()),
                () -> assertTrue(metadata.getResponseTypesSupported().contains("code")),
                () -> assertEquals(0, metadata.getResponseModesSupported().size()),
                () -> assertEquals(1, metadata.getGrantTypesSupported().size()),
                () -> assertTrue(metadata.getGrantTypesSupported().contains("authorization_code")),
                () -> assertEquals(1, metadata.getSubjectTypesSupported().size()),
                () -> assertTrue(metadata.getSubjectTypesSupported().contains("public")),
                () -> assertEquals(3, metadata.getTokenEndpointAuthMethodsSupported().size()),
                () -> assertTrue(metadata.getTokenEndpointAuthMethodsSupported().contains("client_secret_basic")),
                () -> assertTrue(metadata.getTokenEndpointAuthMethodsSupported().contains("client_secret_post")),
                () -> assertTrue(metadata.getTokenEndpointAuthMethodsSupported().contains("client_secret_jwt")),
                () -> assertTrue(metadata.isRequirePushedAuthorizationRequests()),
                () -> assertTrue(metadata.isAuthorizationResponseIssParameterSupported())
        );
    }

    @Test
    @DisplayName("then the response can be parsed and JSON and includes metadata created from the SDK configuration")
    public void testJsonSerialize() throws Exception {
        OpenIDProviderMetadataResponse metadata = OpenIDProviderMetadataResponse.builder()
                .issuer(new URI("https://www.digdir.no/"))
                .pushedAuthorizationRequestEndpoint(new URI("https://www.digdir.no/p"))
                .requirePushedAuthorizationRequests(true)
                .authorizationEndpoint(new URI("https://www.digdir.no/a"))
                .tokenEndpoint(new URI("https://www.digdir.no/t"))
                .jwksUri(new URI("https://www.digdir.no/j"))
                .acrValueSupported("l3")
                .acrValueSupported("l4")
                .uiLocaleSupported("nn")
                .uiLocaleSupported("nb")
                .scopeSupported("openid")
                .scopeSupported("prefix:customscope")
                .claimSupported("claim1")
                .claimSupported("claim2")
                .authorizationDetailsTypeSupported("xxx:1")
                .authorizationDetailsTypeSupported("yyy:2")
                .responseModeSupported("query")
                .responseModeSupported("form_post")
                .idTokenSigningAlgValueSupported("RS256")
                .authorizationSigningAlgValueSupported("RS256")
                .tokenEndpointAuthSigningAlgValueSupported("HS256")
                .authorizationResponseIssParameterSupported(false)
                .build();

        String json = metadata.toJsonString();
        Map<String, Object> jsonMap = JsonUtils.parseJsonObject(json);

        assertAll(
                () -> assertEquals("https://www.digdir.no/", jsonMap.get("issuer")),
                () -> assertEquals("https://www.digdir.no/p", jsonMap.get("pushed_authorization_request_endpoint")),
                () -> assertEquals(true, jsonMap.get("require_pushed_authorization_requests")),
                () -> assertEquals("https://www.digdir.no/a", jsonMap.get("authorization_endpoint")),
                () -> assertEquals("https://www.digdir.no/t", jsonMap.get("token_endpoint")),
                () -> assertEquals("https://www.digdir.no/j", jsonMap.get("jwks_uri")),
                () -> assertEquals(List.of("openid", "prefix:customscope"), jsonMap.get("scopes_supported")),
                () -> assertEquals(List.of("claim1", "claim2"), jsonMap.get("claims_supported")),
                () -> assertEquals(List.of("xxx:1", "yyy:2"), jsonMap.get("authorization_details_types_supported")),
                () -> assertEquals(List.of("code"), jsonMap.get("response_types_supported")),
                () -> assertEquals(List.of("query", "form_post"), jsonMap.get("response_modes_supported")),
                () -> assertEquals(List.of("authorization_code"), jsonMap.get("grant_types_supported")),
                () -> assertEquals(List.of("l3", "l4"), jsonMap.get("acr_values_supported")),
                () -> assertEquals(List.of("public"), jsonMap.get("subject_types_supported")),
                () -> assertEquals(List.of("RS256"), jsonMap.get("id_token_signing_alg_values_supported")),
                () -> assertEquals(List.of("RS256"), jsonMap.get("authorization_signing_alg_values_supported")),
                () -> assertEquals(List.of("client_secret_basic", "client_secret_post", "client_secret_jwt"), jsonMap.get("token_endpoint_auth_methods_supported")),
                () -> assertEquals(List.of("HS256"), jsonMap.get("token_endpoint_auth_signing_alg_values_supported")),
                () -> assertEquals(List.of("nn", "nb"), jsonMap.get("ui_locales_supported")),
                () -> assertFalse((Boolean) jsonMap.get("authorization_response_iss_parameter_supported"))
        );
    }

}
