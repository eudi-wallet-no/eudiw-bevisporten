package no.idporten.eudiw.verifier.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;

@Data
@Validated
public class ClientApplication implements Serializable {
    private String id;
    @NotBlank
    private String keystoreName;
    private boolean disabled = false;
    @NotBlank
    private transient String apiKey;
}
