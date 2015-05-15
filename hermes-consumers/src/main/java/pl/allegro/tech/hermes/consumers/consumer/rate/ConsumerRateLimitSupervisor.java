package pl.allegro.tech.hermes.consumers.consumer.rate;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.boon.collections.ConcurrentHashSet;

public class ConsumerRateLimitSupervisor implements Runnable {

    private final Set<ConsumerRateLimiter> consumerRateLimiters = new ConcurrentHashSet<>();

    @Inject
    public ConsumerRateLimitSupervisor(ConfigFactory configFactory) {
        int period = configFactory.getIntProperty(Configs.CONSUMER_RATE_LIMITER_SUPERVISOR_PERIOD);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this, period, period, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        for (ConsumerRateLimiter limiter : consumerRateLimiters) {
            limiter.adjustConsumerRate();
        }
    }

    public void register(ConsumerRateLimiter consumerRateLimiter) {
        consumerRateLimiters.add(consumerRateLimiter);
    }

    public void unregister(ConsumerRateLimiter consumerRateLimiter) {
        consumerRateLimiters.remove(consumerRateLimiter);
    }
}
