package no.idporten.eudiw.verifier.api.verification;

public class ApiDocProperties {

    public final static String API_TAG = "eudiw-verifier-service-v1";
    public final static String API_DESCRIPTION = "Verifier Service API";

    public final static String PID_MDOC_EXAMPLE = "{\n" +
            "  \"credentials\" : [ {\n" +
            "    \"meta\" : {\n" +
            "      \"doctype_value\" : \"eu.europa.ec.eudi.pid.1\"\n" +
            "    },\n" +
            "    \"format\" : \"mso_mdoc\",\n" +
            "    \"claims\" : [ {\n" +
            "      \"path\" : [ \"eu.europa.ec.eudi.pid.1\", \"family_name\" ]\n" +
            "    }, {\n" +
            "      \"path\" : [ \"eu.europa.ec.eudi.pid.1\", \"given_name\" ]\n" +
            "    }, {\n" +
            "      \"path\" : [ \"eu.europa.ec.eudi.pid.1\", \"birth_date\" ]\n" +
            "    }, {\n" +
            "      \"path\" : [ \"eu.europa.ec.eudi.pid.1\", \"place_of_birth\" ]\n" +
            "    }, {\n" +
            "      \"path\" : [ \"eu.europa.ec.eudi.pid.1\", \"nationality\" ]\n" +
            "    } ],\n" +
            "    \"id\" : \"eudi-pid-mdoc\"\n" +
            "  } ]\n" +
            "}";

}
