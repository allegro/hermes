package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MaxRateRegistry {

    void start();

    void stop();

    Set<ConsumerRateInfo> ensureCorrectAssignments(SubscriptionName subscriptionName, Set<String> currentConsumers);

    void update(SubscriptionName subscriptionName, Map<String, MaxRate> newMaxRates);

    Optional<MaxRate> getMaxRate(ConsumerInstance consumer);

    RateHistory getRateHistory(ConsumerInstance consumer);

    void writeRateHistory(ConsumerInstance consumer, RateHistory rateHistory);
}
