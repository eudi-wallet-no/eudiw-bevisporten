package no.idporten.eudiw.statuslist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication
public class StatusListServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StatusListServiceApplication.class, args);
	}

}
