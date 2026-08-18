package no.idporten.eudiw.issuer.openid4vci.protocol;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(title = "Credential claim", description = "Claim name and value", type = "object")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Claim {

    @Schema(description = "Claim name", example = "foo")
    private String name;
    @Schema(description = "Claim value", example = "bar")
    private String value;

}
