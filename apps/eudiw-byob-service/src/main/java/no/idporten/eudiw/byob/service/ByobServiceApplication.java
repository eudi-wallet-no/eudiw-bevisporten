package no.idporten.eudiw.byob.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication
public class ByobServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ByobServiceApplication.class, args);
	}

}
