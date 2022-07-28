package pl.allegro.tech.hermes.frontend.publishing.handlers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaConfirmed;

public class ThroughputLimiterFactory {

    private final ThroughputParameters throughputParameters;

    private final HermesMetrics hermesMetrics;

    private enum ThroughputLimiterType { UNLIMITED, FIXED, DYNAMIC }

    public ThroughputLimiterFactory(ThroughputParameters throughputParameters, HermesMetrics hermesMetrics) {
        this.throughputParameters = throughputParameters;
        this.hermesMetrics = hermesMetrics;
    }

    public ThroughputLimiter provide() {
        switch (ThroughputLimiterType.valueOf(throughputParameters.getType().toUpperCase())) {
            case UNLIMITED:
                return (a, b) -> quotaConfirmed();
            case FIXED:
                return new FixedThroughputLimiter(throughputParameters.getFixedMax());
            case DYNAMIC:
                return new DynamicThroughputLimiter(
                        throughputParameters.getDynamicMax(),
                        throughputParameters.getDynamicThreshold(),
                        throughputParameters.getDynamicDesired(),
                        throughputParameters.getDynamicIdle(),
                        throughputParameters.getDynamicCheckInterval(),
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
}
