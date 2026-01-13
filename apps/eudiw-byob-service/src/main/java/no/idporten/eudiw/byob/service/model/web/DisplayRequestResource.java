package no.idporten.eudiw.byob.service.model.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import no.idporten.eudiw.byob.service.model.Display;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DisplayRequestResource(
        @NotBlank(message = "Display name is blank")
        @JsonProperty("name")
        String name,
        @NotBlank(message = "Display locale is blank")
        @JsonProperty("locale")
        String locale,
        @JsonProperty("background_color")
        String backgroundColor,
        @JsonProperty("text_color")
        String textColor
) {
    public Display toDisplay() {
        return new Display(
                this.name,
                this.locale,
                this.backgroundColor,
                this.textColor
        );
    }
}
