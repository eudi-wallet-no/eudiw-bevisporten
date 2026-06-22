package no.idporten.eudiw.verifier.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class ClientApplication {
    private String id;
    @NotBlank
    private String keystoreName;
}
