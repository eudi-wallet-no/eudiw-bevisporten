package no.idporten.eudiw.bevisgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication
public class BevisgeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(BevisgeneratorApplication.class, args);
	}

}
