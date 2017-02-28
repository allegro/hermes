package pl.allegro.tech.hermes.common.hook;

import ch.qos.logback.classic.LoggerContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.LoggerFactory;

public class FlushLogsShutdownHook implements ServiceAwareHook {

    @Override
    public void accept(ServiceLocator serviceLocator) {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
    }
}
