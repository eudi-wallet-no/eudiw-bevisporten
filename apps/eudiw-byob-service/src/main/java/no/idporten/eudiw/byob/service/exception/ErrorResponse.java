package no.idporten.eudiw.byob.service.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.opentelemetry.api.trace.Span;

public record ErrorResponse(String error, @JsonProperty("error_description") String errorDescription) {

    public ErrorResponse(String error, String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription + " (trace_id: %s)".formatted(getTraceId());
    }

    private String getTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }
}
