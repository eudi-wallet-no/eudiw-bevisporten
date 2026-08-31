package no.idporten.eudiw.verifier.statuslist;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.net.URI;

public record StatusMDoc(
        @NotBlank String idx,
        @URL URI uri
) {
}