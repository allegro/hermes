package pl.allegro.tech.hermes.consumers.hooks;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class SpringFlushLogsShutdownHook implements SpringServiceAwareHook {

    @Override
    public void accept(ApplicationContext applicationContext) {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
    }
}
