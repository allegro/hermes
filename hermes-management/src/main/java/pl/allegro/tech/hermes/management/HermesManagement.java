package pl.allegro.tech.hermes.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan("pl.allegro.tech.hermes.management")
public class HermesManagement {
    public static void main(String[] args) {
        SpringApplication.run(HermesManagement.class, args);
    }
}
