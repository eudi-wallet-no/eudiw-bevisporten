package no.idporten.eudiw.issuer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class ClasspathCredentialConfigurationSource implements CredentialConfigurationSource {

    private static final Logger log = LoggerFactory.getLogger(ClasspathCredentialConfigurationSource.class);

    @Override
    public List<ExtendedCredentialConfiguration> retrieve(URI uri) throws IOException {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(uri.toString());
        JsonMapper jsonMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .build();
        ExtendedCredentialConfiguration extendedCredentialConfiguration = jsonMapper.readValue(resource.getInputStream(), ExtendedCredentialConfiguration.class);
        log.info("Retrieved credential configuration for {} from classpath {}", extendedCredentialConfiguration.getCredentialType(), uri);
        return List.of(extendedCredentialConfiguration);
    }

}
