package pl.allegro.tech.hermes.consumers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HermesConsumers {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(HermesConsumers.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    application.run(args);
  }
}
