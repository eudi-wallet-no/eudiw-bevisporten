package no.idporten.eudiw.byob.service.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.byob.service.model.Display;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DisplayData(
        @JsonProperty("name")
        String name,
        @JsonProperty("locale")
        String locale,
        @JsonProperty("background_color")
        String backgroundColor,
        @JsonProperty("text_color")
        String textColor
) implements Serializable {

    public DisplayData(Display other){
        this(
                other.name(),
                other.locale(),
                other.backgroundColor(),
                other.textColor()
        );
    }

    public Display toDisplay(){
        return new Display(
                this.name,
                this.locale,
                this.backgroundColor,
                this.textColor
        );
    }
}
