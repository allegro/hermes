package pl.allegro.tech.hermes.frontend.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TopicMetadataLoadingJob implements Runnable {

    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;
    private final ScheduledExecutorService executorService;
    private final int intervalSeconds;

    private ScheduledFuture job;

    @Inject
    public TopicMetadataLoadingJob(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                   ConfigFactory config) {
        this(topicMetadataLoadingRunner, config.getIntProperty(Configs.FRONTEND_TOPIC_METADATA_REFRESH_JOB_INTERVAL_SECONDS));
    }

    TopicMetadataLoadingJob(TopicMetadataLoadingRunner topicMetadataLoadingRunner, int intervalSeconds) {
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
        this.intervalSeconds = intervalSeconds;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("TopicMetadataLoadingJob-%d").build();
        this.executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public void run() {
        topicMetadataLoadingRunner.refreshMetadata();
    }

    public void start() {
        job = executorService.scheduleAtFixedRate(this, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() throws InterruptedException {
        job.cancel(false);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }


}