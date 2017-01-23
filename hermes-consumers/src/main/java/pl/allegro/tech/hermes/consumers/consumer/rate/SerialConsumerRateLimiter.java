package pl.allegro.tech.hermes.consumers.consumer.rate;

import com.google.common.util.concurrent.RateLimiter;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculationResult;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculator;

import java.util.Objects;

public class SerialConsumerRateLimiter implements ConsumerRateLimiter {

    private Subscription subscription;

    private final HermesMetrics hermesMetrics;

    private final ConsumerRateLimitSupervisor rateLimitSupervisor;

    private final RateLimiter rateLimiter;

    private final RateLimiter filterRateLimiter;

    private final OutputRateCalculator outputRateCalculator;

    private final DeliveryCounters deliveryCounters = new DeliveryCounters();

    private OutputRateCalculator.Mode currentMode;

    public SerialConsumerRateLimiter(Subscription subscription, OutputRateCalculator outputRateCalculator,
                                     HermesMetrics hermesMetrics, ConsumerRateLimitSupervisor rateLimitSupervisor) {

        this.subscription = subscription;
        this.hermesMetrics = hermesMetrics;
        this.rateLimitSupervisor = rateLimitSupervisor;
        this.outputRateCalculator = outputRateCalculator;
        this.currentMode = OutputRateCalculator.Mode.NORMAL;
        this.rateLimiter = RateLimiter.create(calculateInitialRate().rate());
        this.filterRateLimiter = RateLimiter.create(subscription.getSerialSubscriptionPolicy().getRate());
    }

    @Override
    public void initialize() {
        adjustConsumerRate();
        hermesMetrics.registerOutputRateGauge(subscription.getTopicName(), subscription.getName(), rateLimiter::getRate);
        rateLimitSupervisor.register(this);
    }

    @Override
    public void shutdown() {
        hermesMetrics.unregisterOutputRateGauge(subscription.getTopicName(), subscription.getName());
        rateLimitSupervisor.unregister(this);
    }

    @Override
    public void acquire() {
        rateLimiter.acquire();
    }

    @Override
    public void acquireFiltered() {
        filterRateLimiter.acquire();
    }

    @Override
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

    @Override
    public void updateSubscription(Subscription newSubscription) {
        this.subscription = newSubscription;
        this.filterRateLimiter.setRate(newSubscription.getSerialSubscriptionPolicy().getRate());
    }

    @Override
    public void registerSuccessfulSending() {
        deliveryCounters.incrementSuccesses();
    }

    @Override
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

        SerialConsumerRateLimiter that = (SerialConsumerRateLimiter) o;

        return Objects.equals(subscription.getQualifiedName(), that.subscription.getQualifiedName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subscription.getQualifiedName());
    }
}
