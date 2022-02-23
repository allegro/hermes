package pl.allegro.tech.hermes.frontend.publishing.handlers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaConfirmed;

public class ThroughputLimiterFactory {//TODO - remove factory or keep it?
    private ConfigFactory configs;
    private HermesMetrics hermesMetrics;

    private enum ThroughputLimiterType { UNLIMITED, FIXED, DYNAMIC }

    @Inject
    public ThroughputLimiterFactory(ConfigFactory configs, HermesMetrics hermesMetrics) {
        this.configs = configs;
        this.hermesMetrics = hermesMetrics;
    }

    public ThroughputLimiter provide() {
        switch (ThroughputLimiterType.valueOf(configs.getStringProperty(Configs.FRONTEND_THROUGHPUT_TYPE).toUpperCase())) {
            case UNLIMITED:
                return (a, b) -> quotaConfirmed();
            case FIXED:
                return new FixedThroughputLimiter(configs.getLongProperty(Configs.FRONTEND_THROUGHPUT_FIXED_MAX));
            case DYNAMIC:
                return new DynamicThroughputLimiter(
                        configs.getLongProperty(Configs.FRONTEND_THROUGHPUT_DYNAMIC_MAX),
                        configs.getLongProperty(Configs.FRONTEND_THROUGHPUT_DYNAMIC_THRESHOLD),
                        configs.getLongProperty(Configs.FRONTEND_THROUGHPUT_DYNAMIC_DESIRED),
                        configs.getDoubleProperty(Configs.FRONTEND_THROUGHPUT_DYNAMIC_IDLE),
                        configs.getIntProperty(Configs.FRONTEND_THROUGHPUT_DYNAMIC_CHECK_INTERVAL),
                        hermesMetrics.meter(Meters.THROUGHPUT_BYTES),
                        getExecutor()
                        );
            default:
                throw new IllegalArgumentException("Unknown throughput limiter type.");
        }
    }

    private ScheduledExecutorService getExecutor() {
        Logger logger = LoggerFactory.getLogger(ThroughputLimiterFactory.class);
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ThroughputLimiterExecutor-%d")
                .setUncaughtExceptionHandler((t, e) -> logger.error("ThroughputLimiterExecutor failed {}", t.getName(), e)).build();
        return newScheduledThreadPool(1, threadFactory);
    }

//    @Override
//    public void dispose(ThroughputLimiter instance) {
//
//    }
}
