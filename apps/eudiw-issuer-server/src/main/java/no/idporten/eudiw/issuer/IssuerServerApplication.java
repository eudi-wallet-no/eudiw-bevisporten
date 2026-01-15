package no.idporten.eudiw.issuer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@EnableScheduling
@SpringBootApplication
public class IssuerServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IssuerServerApplication.class, args);
		addBouncyCastleProvider();
	}

	/**
	 * Bootstrap Bouncy Castle.
	 */
	private static void addBouncyCastleProvider() {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

}
