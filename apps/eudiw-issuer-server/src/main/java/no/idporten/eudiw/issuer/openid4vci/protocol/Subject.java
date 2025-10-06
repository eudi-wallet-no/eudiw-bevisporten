package no.idporten.eudiw.issuer.openid4vci.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Subject for credential issue", title = "Subject", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subject {

    @NotEmpty(message = "Subject identifier must have a value.")
    @Schema(description = "Subject identifier.", example = "12345678901")
    @JsonProperty("identifier")
    private String identifier;

}
