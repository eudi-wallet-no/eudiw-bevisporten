package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Validated
public class CredentialSigningAlgorithms {

    @NotEmpty
    private List<String> dcSdJwt;
    @NotEmpty
    private List<Integer> msoMdoc;

}
