package no.idporten.eudiw.verifier.certificateauthority;

import jakarta.validation.constraints.NotEmpty;

public record CertificateAuthorityCertificate(
        @NotEmpty String certificate
) {
}
