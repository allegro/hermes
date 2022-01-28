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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG;

@SpringBootApplication
public class HermesConsumersApp {

    public static ConfigurableApplicationContext applicationContext;

//    public static void main(String[] args) {
//        List<String> list = new ArrayList<>();
//        list.add("-e=integration");
//        list.add("--test=valueTest");
//        list.add("--" + KAFKA_CONSUMER_RETRY_BACKOFF_MS_CONFIG.getName() + "=" + 69);
//        String[] args2 = list.toArray(new String[0]);
//        main2(args2);
//    }

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
