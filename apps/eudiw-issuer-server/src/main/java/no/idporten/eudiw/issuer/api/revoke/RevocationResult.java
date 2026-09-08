package no.idporten.eudiw.issuer.api.revoke;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Revocation outcome.")
public record RevocationResult(
        @Schema(description = "Number of issuance transactions actually revoked, 0 when nothing matched.", example = "1")
        int revokedCount
) {
}
