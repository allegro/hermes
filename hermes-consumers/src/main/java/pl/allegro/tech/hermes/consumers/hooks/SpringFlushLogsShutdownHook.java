package pl.allegro.tech.hermes.consumers.hooks;

import ch.qos.logback.classic.LoggerContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;

public class SpringFlushLogsShutdownHook implements SpringServiceAwareHook {

    @Override
    public void accept(ApplicationContext applicationContext) {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
    }
}
