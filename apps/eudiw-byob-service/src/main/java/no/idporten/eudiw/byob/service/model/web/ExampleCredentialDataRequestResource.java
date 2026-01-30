package no.idporten.eudiw.byob.service.model.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExampleCredentialDataRequestResource extends HashMap<@NotEmpty(message = "Claim name must hava a value") String, Serializable> {

}
