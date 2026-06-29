package no.idporten.eudiw.verifier.api.verification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;

@Schema(title = "Start verification request", description = "Start credential verification request.  Use client application id and define dcql query", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public record StartVerificationRequest(

        @Schema(description= "dcql query.", example= "{\n" +
                "  \"credentials\" : [ {\n" +
                "    \"meta\" : {\n" +
                "      \"vct_values\" : [ \"no:kontaktregisteret:kontaktinformasjon:1\" ]\n" +
                "    },\n" +
                "    \"format\" : \"dc+sd-jwt\",\n" +
                "    \"claims\" : [ {\n" +
                "      \"path\" : [ \"personidentifikator\" ]\n" +
                "    }, {\n" +
                "      \"path\" : [ \"epostadresse\" ]\n" +
                "    }, {\n" +
                "      \"path\" : [ \"mobiltelefonnummer\" ]\n" +
                "    } ],\n" +
                "    \"id\" : \"kontaktregisteret\"\n" +
                "  } ]\n" +
                "}")
        @JsonProperty("dcql_query")
        DcqlQuery dcqlQuery
) {

}
