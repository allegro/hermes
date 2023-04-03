package pl.allegro.tech.hermes.frontend.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DefaultReadinessChecker implements ReadinessChecker, NodeCacheListener {
    private static final Logger logger = LoggerFactory.getLogger(DefaultReadinessChecker.class);

    private final boolean enabled;
    private final boolean kafkaCheckEnabled;
    private final Duration interval;
    private final TopicMetadataLoadingRunner topicMetadataLoadingRunner;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper mapper;
    private final NodeCache cache;

    private volatile boolean adminReady = false;
    private volatile boolean ready = false;

    public DefaultReadinessChecker(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                   CuratorFramework curator,
                                   ZookeeperPaths paths,
                                   ObjectMapper mapper,
                                   boolean enabled,
                                   boolean kafkaCheckEnabled,
                                   Duration interval) {
        this.enabled = enabled;
        this.kafkaCheckEnabled = kafkaCheckEnabled;
        this.interval = interval;
        this.topicMetadataLoadingRunner = topicMetadataLoadingRunner;
        this.mapper = mapper;
        this.cache = new NodeCache(curator, paths.frontendReadinessPath());
        cache.getListenable().addListener(this);
        try {
            cache.start(true);
        } catch (Exception e) {
            throw new InternalProcessingException("Readiness cache cannot start.", e);
        }
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
            refreshAdminReady();
            ReadinessCheckerJob job = new ReadinessCheckerJob();
            job.run();
            scheduler.scheduleAtFixedRate(job, interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() throws InterruptedException {
        scheduler.shutdown();
        scheduler.awaitTermination(1, TimeUnit.MINUTES);
        try {
            cache.close();
        } catch (Exception e) {
            logger.warn("Failed to stop readiness cache", e);
        }
    }

    @Override
    public void nodeChanged() {
        refreshAdminReady();
    }

    private void refreshAdminReady() {
        try {
            ChildData nodeData = cache.getCurrentData();
            if (nodeData != null) {
                byte[] data = nodeData.getData();
                Readiness value = mapper.readValue(data, Readiness.class);
                adminReady = value.isReady();
            } else {
                adminReady = true;
            }
        } catch (Exception e) {
            logger.error("Failed reloading readiness cache. Current value: ready=" + ready, e);
        }
    }

    private class ReadinessCheckerJob implements Runnable {
        private volatile boolean kafkaReady = false;

        @Override
        public void run() {
            if (!adminReady) {
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
