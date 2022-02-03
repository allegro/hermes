package pl.allegro.tech.hermes.consumers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class HermesConsumersApp {

    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(HermesConsumersApp.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        applicationContext = application.run(args);
    }

    public static HermesConsumers runAndGetInstance(String[] args) {
        main(args);
        return applicationContext.getBean(HermesConsumers.class);
    }
}
