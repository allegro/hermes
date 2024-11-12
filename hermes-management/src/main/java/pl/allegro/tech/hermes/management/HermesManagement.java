package pl.allegro.tech.hermes.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HermesManagement {

  public static void main(String[] args) {
    SpringApplication.run(HermesManagement.class, args);
  }
}
