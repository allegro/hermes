package pl.allegro.tech.hermes.frontend.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.hook.Hook;

public abstract class AbstractShutdownHook implements Hook.Shutdown {

    private static final Logger logger = LoggerFactory.getLogger(AbstractShutdownHook.class);

    public abstract void shutdown() throws InterruptedException;

    @Override
    public void apply() {
        try {
            shutdown();
        } catch (InterruptedException e) {
            logger.error("Exception while shutdown Hermes Frontend", e);
        }
    }
}