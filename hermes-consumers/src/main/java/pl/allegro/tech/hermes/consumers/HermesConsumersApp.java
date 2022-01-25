package pl.allegro.tech.hermes.consumers;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class HermesConsumersApp {

    private static GenericApplicationContext staticApplicationContext;

    @Autowired
    public HermesConsumersApp(GenericApplicationContext applicationContext) {
        staticApplicationContext = applicationContext;//TODO: just a hack, change later
    }

    public static void main(String[] args) {
        SpringApplication.run(HermesConsumersApp.class, args);
        HermesConsumers.main(staticApplicationContext);
    }
}
