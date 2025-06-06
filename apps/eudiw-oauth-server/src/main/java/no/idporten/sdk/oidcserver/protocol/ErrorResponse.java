package no.idporten.sdk.oidcserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import no.idporten.sdk.oidcserver.util.JsonUtils;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ErrorResponse implements JsonResponse {

    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String STATE = "state";

    @JsonProperty(value = ERROR)
    private String error;
    @JsonProperty(value = ERROR_DESCRIPTION)
    private String errorDescription;
    @JsonProperty(value = STATE)
    private String state;

    @Builder
    ErrorResponse(String error, String errorDescription, String state) {
        this.error = error;
        this.errorDescription = (errorDescription == null ? "" : errorDescription);
        this.state = state;
    }

    @Override
    public Map<String, Object> toJsonObject() {
        return JsonUtils.jsonObjectBuilder()
                .addAttribute(ERROR, error)
                .addAttribute(ERROR_DESCRIPTION, errorDescription)
                .addAttribute(STATE, state)
                .build();
    }

}
