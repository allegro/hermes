package pl.allegro.tech.hermes.frontend.server;

import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;

public abstract class AbstractShutdownHook implements ServiceAwareHook {

    private static final Logger logger = LoggerFactory.getLogger(AbstractShutdownHook.class);

    public abstract void shutdown() throws InterruptedException;

    @Override
    public void accept(ServiceLocator serviceLocator) {
        try {
            shutdown();
        } catch (InterruptedException e) {
            logger.error("Exception while shutdown Hermes Frontend", e);
        }
    }
}