package no.idporten.eudiw.bevisgenerator.web.models.advancedForm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.CredentialDefinitionFactory;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinition;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.Display;
import no.idporten.eudiw.bevisgenerator.web.models.unique.UniqueCredentialType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record SimpleCredentialForm(
        @NotBlank(message = "Bevistype er påkravd", groups = CreateForm.class)
        @Pattern(
                regexp = "^[a-z0-9_:.]{3,155}$",
                message = "Bevistype kan berre innehalde små bokstavar, tal, kolon, punktum og understrek. Lengd: 3–155 teikn",
                groups = CreateForm.class
        )
        @UniqueCredentialType(groups = CreateForm.class)
        String credentialType,
        @NotBlank(message = "Format er påkravd", groups = { CreateForm.class, EditForm.class })
        String format,
        @NotBlank(message = "Scope er påkravd", groups = { CreateForm.class, EditForm.class })
        String scope,

        String name,
        @Valid()
        @NotNull(
                message = "Beviset må ha minst eitt claim",
                groups = { CreateForm.class, EditForm.class }
        )
        List<ClaimForm> claims,

        String backgroundColor,
        String textColor,

        String rawJson
) {
    public SimpleCredentialForm() {
        this("", "dc+sd-jwt", CredentialDefinitionFactory.DYNAMIC_CREDENTIAL_SCOPE, "", new ArrayList<>(), null, null, null);
    }

    public SimpleCredentialForm(CredentialDefinition cd) {
        Display display = cd.getCredentialMetadata().display().getFirst();
        String name = display.name();
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

        this(cd.getCredentialType(), cd.getFormat(), cd.getScope(), name, claims,
                display.backgroundColor(), display.textColor(), null);
    }
}
