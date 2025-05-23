package no.idporten.eudiw.issuer.config;

import lombok.Data;

import java.net.URI;

@Data
public class ClaimsSourceProperties {

    private String doctype;
    private URI resourceServer;
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 5000;

}
