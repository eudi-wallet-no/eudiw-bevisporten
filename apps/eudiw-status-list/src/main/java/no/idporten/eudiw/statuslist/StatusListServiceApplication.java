package no.idporten.eudiw.statuslist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication
@EnableScheduling
public class StatusListServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatusListServiceApplication.class, args);
    }

}
