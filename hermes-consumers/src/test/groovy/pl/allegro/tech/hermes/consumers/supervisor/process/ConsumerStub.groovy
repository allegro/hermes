package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset
import pl.allegro.tech.hermes.consumers.consumer.Consumer

class ConsumerStub implements Consumer {

    int initializationCount

    int tearDownCount

    boolean consumptionStarted

    Subscription modifiedSubscription

    @Override
    void consume(Runnable signalsInterrupt) {
        consumptionStarted = true
        signalsInterrupt.run()
    }

    @Override
    void initialize() {
        initializationCount++
    }

    @Override
    void tearDown() {
        tearDownCount++
    }

    @Override
    void updateSubscription(Subscription subscription) {
        modifiedSubscription = subscription
    }

    boolean getInitialized() {
        return initializationCount > 0
    }

    boolean getTornDown() {
        return tearDownCount > 0
    }

    boolean getUpdated() {
        return modifiedSubscription != null
    }
}
