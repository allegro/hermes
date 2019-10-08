package pl.allegro.tech.hermes.frontend.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;

import javax.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class WaitForKafkaStartupHook implements ServiceAwareHook {

    private static final Logger logger = LoggerFactory.getLogger(WaitForKafkaStartupHook.class);

    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;
    private final ScheduledExecutorService scheduler;
    private final RetryPolicy<List<MetadataLoadingResult>> retryPolicy;

    @Inject
    public WaitForKafkaStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner, ConfigFactory config) {
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("wait-kafka-%d").build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.retryPolicy = new RetryPolicy<List<MetadataLoadingResult>>()
                .withMaxRetries(-1)
                .withDelay(Duration.of(config.getLongProperty(Configs.FRONTEND_STARTUP_WAIT_KAFKA_INTERVAL), ChronoUnit.MILLIS))
                .handleIf(this::allMetadataResultsFail);
    }

    private boolean allMetadataResultsFail(List<MetadataLoadingResult> results, Throwable t) {
        return !results.isEmpty() &&  results.stream().allMatch(MetadataLoadingResult::isFailure);
    }

    @Override
    public void accept(ServiceLocator serviceLocator) {
        logger.info("Waiting for Kafka server to start...");
        Failsafe.with(retryPolicy).with(scheduler).get(topicMetadataLoadingRunner::refreshMetadata);
    }

    @Override
    public int getPriority() {
        return Hook.HIGHER_PRIORITY;
    }
}
