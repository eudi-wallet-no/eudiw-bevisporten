package no.idporten.sdk.oidcserver.protocol;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import no.idporten.sdk.oidcserver.util.JsonUtils;

import java.util.Map;

@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UserInfoResponse implements AuditDataProvider, JsonResponse {

    public static final String SUB = "sub";

    @JsonProperty(SUB)
    private String sub;

    @JsonIgnore
    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
        .attribute(SUB, sub)
                .build();
    }

    @Override
    public Map<String, Object> toJsonObject() {
        return JsonUtils.jsonObjectBuilder()
                .addAttribute(SUB, sub)
                .build();
    }
}
