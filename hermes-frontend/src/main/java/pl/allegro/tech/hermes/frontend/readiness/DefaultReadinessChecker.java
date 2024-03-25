package pl.allegro.tech.hermes.frontend.readiness;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.frontend.producer.BrokerTopicAvailabilityChecker;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DefaultReadinessChecker implements ReadinessChecker {

    private final boolean enabled;
    private final boolean topicsCheckEnabled;
    private final Duration interval;
    private final BrokerTopicAvailabilityChecker brokerTopicAvailabilityChecker;
    private final ScheduledExecutorService scheduler;
    private final AdminReadinessService adminReadinessService;

    private volatile boolean ready = false;

    public DefaultReadinessChecker(BrokerTopicAvailabilityChecker brokerTopicAvailabilityChecker,
                                   AdminReadinessService adminReadinessService,
                                   boolean enabled,
                                   boolean topicsCheckEnabled,
                                   Duration interval) {
        this.enabled = enabled;
        this.topicsCheckEnabled = topicsCheckEnabled;
        this.interval = interval;
        this.brokerTopicAvailabilityChecker = brokerTopicAvailabilityChecker;
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
        private volatile boolean allTopicsAvailable = false;

        @Override
        public void run() {
            if (!adminReadinessService.isLocalDatacenterReady()) {
                ready = false;
            } else if (allTopicsAvailable) {
                ready = true;
            } else if (topicsCheckEnabled) {
                allTopicsAvailable = brokerTopicAvailabilityChecker.areAllTopicsAvailable();
                ready = allTopicsAvailable;
            } else {
                allTopicsAvailable = true;
                ready = true;
            }
        }
    }
}
