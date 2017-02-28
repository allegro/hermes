package pl.allegro.tech.hermes.frontend.publishing.handlers;

import com.codahale.metrics.Metered;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.globalQuotaViolation;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaConfirmed;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter.QuotaInsight.quotaViolation;

public class DynamicThroughputLimiter implements ThroughputLimiter, Runnable {
    private final long max;
    private final long threshold;
    private final long desired;
    private final double idleThreshold;
    private final Metered globalThroughputMeter;

    private final ScheduledExecutorService executor;
    private final int checkInterval;

    private ConcurrentHashMap<TopicName, Throughput> users = new ConcurrentHashMap<>();

    public DynamicThroughputLimiter(long max,
                                    long threshold,
                                    long desired,
                                    double idleThreshold,
                                    int checkInterval,
                                    Metered globalThroughput,
                                    ScheduledExecutorService executor) {
        this.max = max;
        this.threshold = threshold;
        this.desired = desired;
        this.idleThreshold = idleThreshold;
        this.globalThroughputMeter = globalThroughput;
        this.checkInterval = checkInterval;
        this.executor = executor;
    }

    @Override
    public QuotaInsight checkQuota(TopicName topic, Metered rate) {
        Throughput throughput = users.computeIfAbsent(topic, name -> new Throughput(rate, max));
        long value = throughput.getRoundedOneMinuteRate();
        if (value > throughput.max) {
            return quotaViolation(value, throughput.max);
        } else if (globalThroughputMeter.getOneMinuteRate() > max) {
            return globalQuotaViolation();
        } else {
            return quotaConfirmed();
        }
    }

    @Override
    public void start() {
        executor.scheduleAtFixedRate(this, checkInterval, checkInterval, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        if (globalThroughputMeter.getOneMinuteRate() > threshold) {
            calibrateLimits();
        }
    }

    private void calibrateLimits() {
        users.entrySet().removeIf(entry -> entry.getValue().getOneMinuteRate() <= idleThreshold);
        int userCount = users.size();
        if (userCount > 0) {
            long total = users.reduceValuesToLong(Long.MAX_VALUE, Throughput::getRoundedOneMinuteRate, 0, ((left, right) -> left + right));
            long mean = total / userCount;
            long desiredMean = desired / userCount;
            users.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getRoundedOneMinuteRate() >= mean)
                    .forEach(entry -> entry.getValue().max = desiredMean);
        }
    }

    private static class Throughput {
        Metered current;
        volatile long max;

        Throughput(Metered current, long max) {
            this.current = current;
            this.max = max;
        }

        long getRoundedOneMinuteRate() {
            return (long) Math.floor(current.getOneMinuteRate());
        }

        double getOneMinuteRate() {
            return current.getOneMinuteRate();
        }
    }
}
