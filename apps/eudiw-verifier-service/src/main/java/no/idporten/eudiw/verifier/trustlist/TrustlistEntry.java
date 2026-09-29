package no.idporten.eudiw.verifier.trustlist;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;

/**
 * A configured trustlist URL together with its wire encoding. The encoding is declared explicitly
 * in configuration (defaulting to XML, since that is by far the most common case) rather than
 * inferred from the URL, since it is known at the time the trustlist is added.
 */
public record TrustlistEntry(@NotNull URI url, @DefaultValue("XML") TrustlistEncoding encoding) {
}
