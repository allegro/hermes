package pl.allegro.tech.hermes.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HermesFrontendApp { //TODO - change name to HermesFrontend

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(HermesFrontendApp.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }
}