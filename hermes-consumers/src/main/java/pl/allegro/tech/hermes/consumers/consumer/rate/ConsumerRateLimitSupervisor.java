package pl.allegro.tech.hermes.consumers.consumer.rate;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ConsumerRateLimitSupervisor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerRateLimitSupervisor.class);

    private final Set<ConsumerRateLimiter> consumerRateLimiters = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject
    public ConsumerRateLimitSupervisor(ConfigFactory configFactory) {
        int period = configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_SUPERVISOR_PERIOD);
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("rate-limit-supervisor-%d").build();
        Executors.newSingleThreadScheduledExecutor(threadFactory)
                .scheduleAtFixedRate(this, period, period, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        for (ConsumerRateLimiter limiter : consumerRateLimiters) {
            try {
                limiter.adjustConsumerRate();
            } catch (Exception e) {
                logger.warn("Issue adjusting consumer rate", e);
            }
        }
    }

    public void register(ConsumerRateLimiter consumerRateLimiter) {
        consumerRateLimiters.add(consumerRateLimiter);
    }

    public void unregister(ConsumerRateLimiter consumerRateLimiter) {
        consumerRateLimiters.remove(consumerRateLimiter);
    }
}
