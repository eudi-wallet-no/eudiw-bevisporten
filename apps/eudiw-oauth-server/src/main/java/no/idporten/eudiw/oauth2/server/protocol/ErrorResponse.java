package no.idporten.eudiw.oauth2.server.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.opentelemetry.api.trace.Span;
import lombok.Builder;
import lombok.Data;
import no.idporten.eudiw.oauth2.server.util.JsonUtils;

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
        this.errorDescription ="%s (trace_id=%s)".formatted(errorDescription == null ? "" : errorDescription, Span.current().getSpanContext().getTraceId());
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
