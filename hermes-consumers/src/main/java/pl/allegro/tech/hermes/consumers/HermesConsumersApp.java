package pl.allegro.tech.hermes.consumers;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class HermesConsumersApp {

    public static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(HermesConsumersApp.class, args);
        getInstance().start();
    }

    public static void stop() {
        getInstance().stop();
    }

    public static HermesConsumers getInstance() {
        return applicationContext.getBean(HermesConsumers.class);
    }
}
