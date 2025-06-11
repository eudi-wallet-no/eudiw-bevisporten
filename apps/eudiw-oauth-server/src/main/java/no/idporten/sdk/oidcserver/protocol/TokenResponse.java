package no.idporten.sdk.oidcserver.protocol;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import no.idporten.sdk.oidcserver.util.JsonObjectBuilder;
import no.idporten.sdk.oidcserver.util.JsonUtils;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class TokenResponse implements AuditDataProvider, JsonResponse {

    public static final String ID_TOKEN = "id_token";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String EXPIRES_IN = "expires_in";

    @JsonProperty(ID_TOKEN)
    private String idToken;

    @JsonProperty(ACCESS_TOKEN)
    private String accessToken;

    @Builder.Default
    @JsonProperty(TOKEN_TYPE)
    private String tokenType = "Bearer";

    @Builder.Default
    @JsonProperty(EXPIRES_IN)
    private long expiresInSeconds = 120;

    @JsonIgnore
    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .attribute(ID_TOKEN, idToken)
                .accessToken(accessToken)
                .attribute(EXPIRES_IN, expiresInSeconds)
                .attribute(TOKEN_TYPE, tokenType)
                .build();
    }

    @Override
    public Map<String, Object> toJsonObject() {
        JsonObjectBuilder jsonObjectBuilder = JsonUtils.jsonObjectBuilder()
                .addAttribute(ACCESS_TOKEN, accessToken)
                .addAttribute(TOKEN_TYPE, tokenType)
                .addAttribute(EXPIRES_IN, expiresInSeconds);
        if (idToken != null) {
            jsonObjectBuilder.addAttribute(ID_TOKEN, idToken);
        }
        return jsonObjectBuilder.build();
    }

}
