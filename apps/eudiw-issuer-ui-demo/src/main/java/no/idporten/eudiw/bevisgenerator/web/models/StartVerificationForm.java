package no.idporten.eudiw.bevisgenerator.web.models;

import jakarta.validation.constraints.NotBlank;

public record StartVerificationForm(
        @NotBlank(message = "Credential configuration må velgast")
        String credentialConfigurationId,
        @NotBlank(message = "DCQL query må ha verdi")
        String dcqlQuery
) {
    public StartVerificationForm() {
        this("", """
                "credentials": [
                    {
                      "meta": {
                        "vct_values": [
                          "no:kontaktregisteret:kontaktinformasjon:1"
                        ]
                      },
                      "format": "dc+sd-jwt",
                      "claims": [
                        {
                          "path": [
                            "personidentifikator"
                          ]
                        },
                        {
                          "path": [
                            "epostadresse"
                          ]
                        },
                        {
                          "path": [
                            "mobiltelefonnummer"
                          ]
                        }
                      ],
                      "id": "kontaktregisteret"
                    }
                  ]
                }""");
    }
}
