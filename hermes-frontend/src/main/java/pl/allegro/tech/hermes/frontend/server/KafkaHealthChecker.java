package pl.allegro.tech.hermes.frontend.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

public class KafkaHealthChecker {

    public enum HEALTH_STATUS {
        FAILURE,
        SUCCESS;

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }

    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;
    private final ScheduledExecutorService scheduler;
    private final RetryPolicy<List<MetadataLoadingResult>> retryPolicy;

    @Inject
    public KafkaHealthChecker(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                              ConfigFactory config) {
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("wait-kafka-%d").build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.retryPolicy = new RetryPolicy<List<MetadataLoadingResult>>()
                .withMaxRetries(-1)
                .withDelay(Duration.of(config.getLongProperty(Configs.FRONTEND_KAFKA_HEALTH_CHECK_INTERVAL), ChronoUnit.MILLIS))
                .withMaxDuration(Duration.of(config.getLongProperty(Configs.FRONTEND_KAFKA_HEALTH_CHECK_WAIT_TIMEOUT), ChronoUnit.MILLIS))
                .handleIf((metadataLoadingResults, throwable) -> areAllMetadataResultsFail(metadataLoadingResults));
    }

    public HEALTH_STATUS waitForKafkaReadiness() {
        List<MetadataLoadingResult> results = Failsafe.with(retryPolicy).with(scheduler).get(topicMetadataLoadingRunner::refreshMetadata);
        return areAllMetadataResultsFail(results) ? HEALTH_STATUS.FAILURE : HEALTH_STATUS.SUCCESS;
    }

    private boolean areAllMetadataResultsFail(List<MetadataLoadingResult> results) {
        return !results.isEmpty() && results.stream().allMatch(MetadataLoadingResult::isFailure);
    }
}
