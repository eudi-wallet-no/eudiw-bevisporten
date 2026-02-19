package no.idporten.eudiw.bevisgenerator.web.models.advancedForm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinition;
import no.idporten.eudiw.bevisgenerator.web.models.unique.UniqueCredentialType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record SimpleCredentialForm(
        @NotBlank(message = "Credential type er påkrevd", groups = CreateForm.class)
        @Pattern(
                regexp = "^[a-z0-9_:]{3,155}",
                message = "Credential type kan kun bestå av små bokstaver, tall, kolon og understrek.\n Lengde: 3-155 tegn",
                groups = CreateForm.class
        )
        @UniqueCredentialType(groups = CreateForm.class)
        String credentialType,
        @NotBlank(message = "Format er påkrevd", groups = { CreateForm.class, EditForm.class })
        String format,
        @NotBlank(message = "Scope er påkrevd", groups = { CreateForm.class, EditForm.class })
        String scope,

        String name,
        @Valid()
        @NotNull(
                message = "Beviset må ha minimum 1. claim",
                groups = { CreateForm.class, EditForm.class }
        )
        List<ClaimForm> claims
) {
    public SimpleCredentialForm() {
        this("", "dc+sd-jwt", "eudiw:eidas2sandkasse:dynamicvc", "", new ArrayList<>());
    }

    public SimpleCredentialForm(CredentialDefinition cd) {
        String name = cd.getCredentialMetadata().display().getFirst().name();
        Map<String, Serializable> exampleData =
            cd.getExampleCredentialData();
        List<ClaimForm> claims = cd
            .getCredentialMetadata()
            .claims()
            .stream()
            .map(claim -> {
                String fieldName = claim.display().getFirst().name();
                String exampleValue = String.valueOf(exampleData.get(claim.path()));
                return new ClaimForm(claim.path(), fieldName, exampleValue);
            }).toList();

        this(cd.getCredentialType(), cd.getFormat(), cd.getScope(), name, claims);
    }
}
