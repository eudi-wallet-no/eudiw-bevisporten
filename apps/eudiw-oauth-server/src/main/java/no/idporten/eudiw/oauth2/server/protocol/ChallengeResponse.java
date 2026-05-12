package no.idporten.eudiw.oauth2.server.protocol;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import no.idporten.eudiw.oauth2.server.util.JsonObjectBuilder;
import no.idporten.eudiw.oauth2.server.util.JsonUtils;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ChallengeResponse implements JsonResponse {

    public static final String ATTESTATION_CHALLENGE = "attestation_challenge";

    @JsonProperty(ATTESTATION_CHALLENGE)
    private String attestationChallenge;

    @Override
    public Map<String, Object> toJsonObject() {
        JsonObjectBuilder jsonObjectBuilder = JsonUtils.jsonObjectBuilder()
                .addAttribute(ATTESTATION_CHALLENGE, attestationChallenge);
        return jsonObjectBuilder.build();
    }

}
