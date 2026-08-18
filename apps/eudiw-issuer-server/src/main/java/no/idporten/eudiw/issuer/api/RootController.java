package no.idporten.eudiw.issuer.api;

import io.swagger.v3.oas.annotations.Hidden;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static no.idporten.eudiw.issuer.api.Endpoints.METADATA_ENDPOINT;
import static no.idporten.eudiw.issuer.api.Endpoints.METADATA_ENDPOINT_TENANT;

/**
 * Controller for applications root URL.  Also handles the root URL for all tenants.
 */
@Hidden
@Controller
public class RootController {

    private final CredentialIssuerTenantService credentialIssuerTenantService;

    public RootController(CredentialIssuerTenantService credentialIssuerTenantService) {
        this.credentialIssuerTenantService = credentialIssuerTenantService;
    }

    /**
     * For any tenant except root, redirect to the tenant specific metadata endpoint.
     */
    @GetMapping(path = {"/{tenant}", "/{tenant}/"})
    public String redirectToCredentialIssuerTenant(@PathVariable("tenant") String tenant) {
        return "redirect:" + metadataEndpointURI(credentialIssuerTenantService.findTenantById(tenant));
    }

    /**
     * For the root path, display an index page with information about this installation of the issuer with links
     * to all credential issuer tenants.
     */
    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index() {

        return """
                <!DOCTYPE html>
                <html lang="en">
                
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Bevisporten EAA-utsteder fra Digdir</title>
                </head>
                <body>
                  <h1>Bevisporten EAA-utsteder fra Digdir</h1>
                
                  <p>Bevisporten er en del av <a href="https://docs.digdir.no/docs/lommebok/lommebok_om.html">Nasjonal sandkasse for digital lommebok</a>.</p>
                
                  <h2>Logiske utstedere</h2>
                  <p>Denne installasjonen av Bevisporten EAA-utsteder har følgende logiske utstedere:</p>
                  <ul>
                      %s
                  </ul>
                
                  <h2>API-dokumentasjon</h2>
                  <p>Utstederens API brukes til å starte utstedelsen av bevis.</p>
                  <ul>
                    <li><a href="%s">OpenAPI definition for extended issuer API</li>
                  </ul>                  
                </body>
                </html>
                """.formatted(listCredentialIssuerTenants(credentialIssuerTenantService.findAllTenants()), Endpoints.OPENAPI_ENDPOINT);
    }

    private String listCredentialIssuerTenants(List<CredentialIssuerTenant> credentialIssuerTenants) {
        return credentialIssuerTenants.stream().map(this::listCredentialIssuerTenants).collect(Collectors.joining());
    }

    private String listCredentialIssuerTenants(CredentialIssuerTenant credentialIssuerTenant) {
        return "<li><a href=\"%s\">%s</a></li>"
                .formatted(
                        metadataEndpointURI(credentialIssuerTenant),
                        displayName(credentialIssuerTenant));
    }

    private URI metadataEndpointURI(CredentialIssuerTenant credentialIssuerTenant) {
        return Endpoints.endpointURI(URI.create(""), credentialIssuerTenant.isRootCredentialIssuer() ? METADATA_ENDPOINT : METADATA_ENDPOINT_TENANT, credentialIssuerTenant.getId());
    }

    private String displayName(CredentialIssuerTenant credentialIssuerTenant) {
        return credentialIssuerTenant.getDisplayNames().getOrDefault("no", credentialIssuerTenant.getDisplayNames().values().stream().findFirst().orElse(credentialIssuerTenant.getCredentialIssuer().toString()));
    }

}
