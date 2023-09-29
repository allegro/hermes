package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class KafkaPartitionLeaderLoadingJob {
    private final ScheduledExecutorService executorService;
    private final Duration interval;
    private final KafkaPartitionLeaderRegistry partitionLeaderRegistry;

    public KafkaPartitionLeaderLoadingJob(
            KafkaPartitionLeaderRegistry partitionLeaderRegistry,
            Duration refreshInterval) {
        this.partitionLeaderRegistry = partitionLeaderRegistry;
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("KafkaPartitionLeaderLoadingJob-%d").build();
        this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.interval = refreshInterval;
    }

    private ScheduledFuture<?> job;

    public void start() {
        job = executorService.scheduleAtFixedRate(partitionLeaderRegistry::updateLeaders, interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS);
    }

    public void stop() throws InterruptedException {
        job.cancel(true);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

}
