package no.idporten.sdk.oidcserver.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import no.idporten.sdk.oidcserver.util.JsonObjectBuilder;

import java.util.Map;

@SuperBuilder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PushedAuthorizationResponse implements AuditDataProvider, JsonResponse {

    public static final String EXPIRES_IN = "expires_in";
    public static final String REQUEST_URI = "request_uri";

    @JsonIgnore
    @Builder.Default
    private int httpStatusCode = 201;

    @JsonProperty(EXPIRES_IN)
    private long expiresIn;

    @JsonProperty(REQUEST_URI)
    private String requestUri;

    @JsonIgnore
    @Override
    public final AuditData getAuditData() {
        AuditData.AuditDataBuilder builder = AuditData.builder();
        return buildAuditData(builder)
                .attribute(EXPIRES_IN, expiresIn)
                .build();
    }

    protected AuditData.AuditDataBuilder buildAuditData(AuditData.AuditDataBuilder builder) {
        return builder.attribute(REQUEST_URI, requestUri);
    }

    @Override
    public final Map<String, Object> toJsonObject() {
        JsonObjectBuilder jsonObjectBuilder = JsonObjectBuilder.builder();
        return buildJsonObject(jsonObjectBuilder)
                .addAttribute(EXPIRES_IN, expiresIn)
                .build();
    }

    protected JsonObjectBuilder buildJsonObject(JsonObjectBuilder builder) {
        return builder
                .addAttribute(REQUEST_URI, requestUri);
    }

}
