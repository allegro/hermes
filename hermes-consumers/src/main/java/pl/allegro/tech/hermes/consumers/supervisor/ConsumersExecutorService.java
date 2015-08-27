package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConsumersExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(ConsumersExecutorService.class);
    private final ThreadPoolExecutor executor;

    @Inject
    public ConsumersExecutorService(ConfigFactory configFactory, HermesMetrics hermesMetrics) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("Consumer-%d")
            .setUncaughtExceptionHandler((t, e) -> logger.error("Exception from consumer with name {}", t.getName(), e)).build();
        int poolSize = configFactory.getIntProperty(Configs.CONSUMER_THREAD_POOL_SIZE);

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize, threadFactory);

        hermesMetrics.registerConsumersThreadGauge(() -> executor.getActiveCount());
    }

    public void execute(Consumer consumer) {
        executor.execute(consumer);
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error("Termination of consumers executor service interrupted.", e);
        }
    }

}
