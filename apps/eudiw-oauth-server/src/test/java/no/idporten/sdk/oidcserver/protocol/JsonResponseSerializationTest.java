package no.idporten.sdk.oidcserver.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.sdk.oidcserver.util.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("when serializing JsonResponse objects with either toJsonString() or Jackson object mapper")
public class JsonResponseSerializationTest {

    void testJsonSerializeEquals(JsonResponse jsonResponse) throws Exception {
        String jsonString = jsonResponse.toJsonString();
        String jacksonJson = new ObjectMapper().writeValueAsString(jsonResponse);
        assertEquals(JsonUtils.parseJsonObject(jsonString), JsonUtils.parseJsonObject(jacksonJson));
        assertEquals(jsonResponse.toJsonObject(), JsonUtils.parseJsonObject(jacksonJson));
        JSONAssert.assertEquals(jsonString, jacksonJson, JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("then serialized token responses are equal")
    void testTokenResponse() throws Exception {
        TokenResponse tokenResponse = TokenResponse.builder()
                .expiresInSeconds(1)
                .tokenType("bjørnar")
                .idToken("id")
                .accessToken("at")
                .build();
        testJsonSerializeEquals(tokenResponse);
    }

    @Test
    @DisplayName("then serialized userinfo responses are equal")
    void testUserinfoResponse() throws Exception {
        UserInfoResponse userInfoResponse = UserInfoResponse.builder().sub("s").build();
        testJsonSerializeEquals(userInfoResponse);
    }

    @Test
    @DisplayName("then serialized error responses are equal")
    void testErrorResponse() throws Exception {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("invalid_something")
                .errorDescription("ed")
                .state("s")
                .build();
        testJsonSerializeEquals(errorResponse);
    }

    @Test
    @DisplayName("then serialized openid provider metadata responses are equal")
    void testOpenIDProviderMetadata() throws Exception {
        OpenIDProviderMetadataResponse metadata = OpenIDProviderMetadataResponse.builder().build();
        testJsonSerializeEquals(metadata);
    }

}
