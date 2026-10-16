package no.idporten.eudiw.verifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.Security;


@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
public class VerifierServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VerifierServiceApplication.class, args);
		addBouncyCastleProvider();

	}

	/**
	 * Bootstrap Bouncy Castle.
	 */
	private static void addBouncyCastleProvider() {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

}
