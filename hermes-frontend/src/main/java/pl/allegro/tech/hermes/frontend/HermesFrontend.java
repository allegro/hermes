package pl.allegro.tech.hermes.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HermesFrontend {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(HermesFrontend.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    application.run(args);
  }
}
