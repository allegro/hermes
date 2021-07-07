package pl.allegro.tech.hermes.frontend.server;

import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;

import javax.inject.Inject;

public class WaitForKafkaStartupHook implements ServiceAwareHook {

    private static final Logger logger = LoggerFactory.getLogger(WaitForKafkaStartupHook.class);
    private final KafkaHealthChecker kafkaHealthChecker;

    @Inject
    public WaitForKafkaStartupHook(KafkaHealthChecker kafkaHealthChecker) {
        this.kafkaHealthChecker = kafkaHealthChecker;
    }

    @Override
    public void accept(ServiceLocator serviceLocator) {
        logger.info("Waiting for Kafka server to start...");
        kafkaHealthChecker.waitForKafkaReadiness();
    }

    @Override
    public int getPriority() {
        return Hook.HIGHER_PRIORITY;
    }
}
