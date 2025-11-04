package no.idporten.eudiw.verifier.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication
public class VerifierProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(VerifierProxyApplication.class, args);
	}

}
