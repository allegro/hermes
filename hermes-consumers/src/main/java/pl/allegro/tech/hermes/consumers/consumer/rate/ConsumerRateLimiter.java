package pl.allegro.tech.hermes.consumers.consumer.rate;

import com.codahale.metrics.Gauge;
import com.google.common.util.concurrent.RateLimiter;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculationResult;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator;

public class ConsumerRateLimiter {

    private Subscription subscription;

    private final HermesMetrics hermesMetrics;

    private final ConsumerRateLimitSupervisor rateLimitSupervisor;

    private final RateLimiter rateLimiter;

    private final OutputRateCalculator outputRateCalculator;

    private final DeliveryCounters deliveryCounters = new DeliveryCounters();

    private OutputRateCalculator.Mode currentMode;

    public ConsumerRateLimiter(Subscription subscription, OutputRateCalculator outputRateCalculator,
            HermesMetrics hermesMetrics, ConsumerRateLimitSupervisor rateLimitSupervisor) {

        this.subscription = subscription;
        this.hermesMetrics = hermesMetrics;
        this.rateLimitSupervisor = rateLimitSupervisor;
        this.outputRateCalculator = outputRateCalculator;
        this.currentMode = OutputRateCalculator.Mode.NORMAL;
        this.rateLimiter = RateLimiter.create(calculateInitialRate().rate());
    }

    public void initialize() {
        adjustConsumerRate();
        hermesMetrics.registerOutputRateGauge(subscription.getTopicName(), subscription.getName(), new Gauge<Double>() {
            @Override
            public Double getValue() {
                return rateLimiter.getRate();
            }
        });
        rateLimitSupervisor.register(this);
    }

    public void shutdown() {
        hermesMetrics.unregisterOutputRateGauge(subscription.getTopicName(), subscription.getName());
        rateLimitSupervisor.unregister(this);
    }

    public void acquire() {
        rateLimiter.acquire();
    }

    public void adjustConsumerRate() {
        OutputRateCalculationResult result = recalculate();
        rateLimiter.setRate(result.rate());
        currentMode = result.mode();
        deliveryCounters.reset();
    }

    private OutputRateCalculationResult calculateInitialRate() {
        return outputRateCalculator.recalculateRate(subscription, deliveryCounters, currentMode, 0.0);
    }

    private OutputRateCalculationResult recalculate() {
        return outputRateCalculator.recalculateRate(subscription, deliveryCounters, currentMode, rateLimiter.getRate());
    }

    public void updateSubscription(Subscription newSubscription) {
        this.subscription = newSubscription;
    }

    public void registerSuccessfulSending() {
        deliveryCounters.incrementSuccesses();
    }

    public void registerFailedSending() {
        deliveryCounters.incrementFailures();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConsumerRateLimiter that = (ConsumerRateLimiter) o;

        return subscription.equals(that.subscription);
    }

    @Override
    public int hashCode() {
        return subscription.hashCode();
    }
}
