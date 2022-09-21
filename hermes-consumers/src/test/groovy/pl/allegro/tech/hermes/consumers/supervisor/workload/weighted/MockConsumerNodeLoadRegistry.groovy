package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder

class MockConsumerNodeLoadRegistry implements ConsumerNodeLoadRegistry {

    private final Map<String, ConsumerNodeLoad> loads = new HashMap<>()

    @Override
    SubscriptionLoadRecorder register(SubscriptionName subscriptionName) {
        throw new UnsupportedOperationException()
    }

    @Override
    void start() {
        throw new UnsupportedOperationException()
    }

    @Override
    void stop() {
        throw new UnsupportedOperationException()
    }

    @Override
    ConsumerNodeLoad get(String consumerId) {
        return loads.getOrDefault(consumerId, ConsumerNodeLoad.UNDEFINED)
    }

    MockConsumerNodeLoadRegistry operationsPerSecond(SubscriptionName subscriptionName, Map<String, Double> opsPerConsumer) {
        opsPerConsumer.entrySet().each {
            ConsumerNodeLoad consumerNodeLoad = loads.getOrDefault(it.key, new ConsumerNodeLoad(1d, [:]))
            consumerNodeLoad.loadPerSubscription.put(subscriptionName, new SubscriptionLoad(it.value))
            loads.put(it.key, consumerNodeLoad)
        }
        return this
    }

    void reset() {
        loads.clear()
    }
}
