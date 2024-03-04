package pl.allegro.tech.hermes.frontend.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TopicMetadataLoadingJob implements Runnable {

    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;
    private final ScheduledExecutorService executorService;
    private final Duration interval;

    private ScheduledFuture<?> job;

    public TopicMetadataLoadingJob(TopicMetadataLoadingRunner topicMetadataLoadingRunner, Duration interval) {
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
        this.interval = interval;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("TopicMetadataLoadingJob-%d").build();
        this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public void run() {
        topicMetadataLoadingRunner.refreshMetadataForAllDatacenters();
    }

    public void start() {
        job = executorService.scheduleAtFixedRate(this, interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS);
    }

    public void stop() throws InterruptedException {
        job.cancel(false);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
}
