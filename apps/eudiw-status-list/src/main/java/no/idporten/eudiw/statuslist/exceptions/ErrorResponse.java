package no.idporten.eudiw.statuslist.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.opentelemetry.api.trace.Span;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(@JsonProperty("error") String error, @JsonProperty("error_description") String errorDescription) {

    public ErrorResponse(String error, String errorDescription) {
        this.error = error;
        this.errorDescription ="%s (trace_id=%s)".formatted(errorDescription, Span.current().getSpanContext().getTraceId());
    }

}
