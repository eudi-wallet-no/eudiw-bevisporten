package no.idporten.sdk.oidcserver.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import no.idporten.sdk.oidcserver.util.JsonObjectBuilder;

@SuperBuilder
@Getter
@AllArgsConstructor
public class DirectPushedAuthorizationResponse extends PushedAuthorizationResponse implements AuditDataProvider, JsonResponse {

    public static final String ID_TOKEN = "id_token";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String STATE = "state";

    @JsonIgnore
    @Builder.Default
    private int httpStatusCode = 200;

    @JsonProperty(ID_TOKEN)
    private String idToken;

    @JsonProperty(ACCESS_TOKEN)
    private String accessToken;

    @Builder.Default
    @JsonProperty(TOKEN_TYPE)
    private String tokenType = "Bearer";

    @JsonProperty(STATE)
    private String state;

    @JsonIgnore
    @Override
    protected AuditData.AuditDataBuilder buildAuditData(AuditData.AuditDataBuilder builder) {
        return builder
                .attribute(ID_TOKEN, idToken)
                .accessToken(accessToken)
                .attribute(TOKEN_TYPE, tokenType)
                .attribute(STATE, state);
    }

    @JsonIgnore
    @Override
    public JsonObjectBuilder buildJsonObject(JsonObjectBuilder builder) {
        return builder
                .addAttribute(ID_TOKEN, idToken)
                .addAttribute(ACCESS_TOKEN, accessToken)
                .addAttribute(TOKEN_TYPE, tokenType)
                .addAttribute(STATE, state);
    }

}
