package pl.allegro.tech.hermes.frontend.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_READINESS_CHECK_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_READINESS_CHECK_INTERVAL_SECONDS;

public class ReadinessChecker implements IReadinessChecker {
    private static final Logger logger = LoggerFactory.getLogger(ReadinessChecker.class);

    private final boolean enabled;
    private final int intervalSeconds;
    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;
    private final ReadinessRepository readinessRepository;
    private final ScheduledExecutorService scheduler;

    private volatile boolean ready = false;

    @Inject
    public ReadinessChecker(ConfigFactory config,
                            TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                            ReadinessRepository readinessRepository) {
        this.enabled = config.getBooleanProperty(FRONTEND_READINESS_CHECK_ENABLED);
        this.intervalSeconds = config.getIntProperty(FRONTEND_READINESS_CHECK_INTERVAL_SECONDS);
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
        this.readinessRepository = readinessRepository;
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
            scheduler.scheduleAtFixedRate(job, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
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
            if (!readinessRepository.isReady()) {
                ready = false;
            } else if (kafkaReady) {
                ready = true;
            } else {
                kafkaReady = checkKafkaReadiness();
                ready = kafkaReady;
            }
        }

        private boolean checkKafkaReadiness() {
            try {
                List<MetadataLoadingResult> results = topicMetadataLoadingRunner.refreshMetadata();
                return results.stream().noneMatch(MetadataLoadingResult::isFailure);
            } catch (Exception ex) {
                logger.warn("Unexpected error occurred during checking Kafka readiness", ex);
                return false;
            }
        }
    }
}
