package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ProducerMetadataLoadingJob implements Runnable {

    private final KafkaMessageSenders kafkaMessageSenders;
    private final ScheduledExecutorService executorService;
    private final boolean enabled;
    private final Duration interval;

    private ScheduledFuture<?> job;

    public ProducerMetadataLoadingJob(KafkaMessageSenders kafkaMessageSenders,
                                      boolean enabled,
                                      Duration interval) {
        this.kafkaMessageSenders = kafkaMessageSenders;
        this.enabled = enabled;
        this.interval = interval;
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("TopicMetadataLoadingJob-%d").build();
        this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public void run() {
        kafkaMessageSenders.refreshTopicMetadata();
    }

    public void start() {
        if (enabled) {
            kafkaMessageSenders.refreshTopicMetadata();
            job = executorService.scheduleAtFixedRate(this, interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS);
        }
    }

    public void stop() throws InterruptedException {
        if (enabled) {
            job.cancel(false);
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        }
    }
}
