package pl.allegro.tech.hermes.frontend.readiness;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.server.MetadataLoadingResult;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingRunner;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DefaultReadinessChecker implements ReadinessChecker {
    private static final Logger logger = LoggerFactory.getLogger(DefaultReadinessChecker.class);

    private final boolean enabled;
    private final boolean kafkaCheckEnabled;
    private final Duration interval;
    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;
    private final ScheduledExecutorService scheduler;
    private final AdminReadinessService adminReadinessService;

    private volatile boolean ready = false;

    public DefaultReadinessChecker(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                   AdminReadinessService adminReadinessService,
                                   boolean enabled,
                                   boolean kafkaCheckEnabled,
                                   Duration interval) {
        this.enabled = enabled;
        this.kafkaCheckEnabled = kafkaCheckEnabled;
        this.interval = interval;
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
        this.adminReadinessService = adminReadinessService;
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ReadinessChecker-%d").build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public boolean isReady() {
        if (!enabled) {
            return true;
        }
        return ready;
    }

    @Override
    public void start() {
        if (enabled) {
            ReadinessCheckerJob job = new ReadinessCheckerJob();
            job.run();
            scheduler.scheduleAtFixedRate(job, interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() throws InterruptedException {
        scheduler.shutdown();
        scheduler.awaitTermination(1, TimeUnit.MINUTES);
    }

    private class ReadinessCheckerJob implements Runnable {
        private volatile boolean kafkaReady = false;

        @Override
        public void run() {
            if (!adminReadinessService.isLocalDatacenterReady()) {
                ready = false;
            } else if (kafkaReady) {
                ready = true;
            } else {
                kafkaReady = checkKafkaReadiness();
                ready = kafkaReady;
            }
        }

        private boolean checkKafkaReadiness() {
            if (kafkaCheckEnabled) {
                try {
                    List<MetadataLoadingResult> results = topicMetadataLoadingRunner.refreshMetadata();
                    return results.stream().noneMatch(MetadataLoadingResult::isFailure);
                } catch (Exception ex) {
                    logger.warn("Unexpected error occurred during checking Kafka readiness", ex);
                    return false;
                }
            }
            return true;
        }
    }
}
