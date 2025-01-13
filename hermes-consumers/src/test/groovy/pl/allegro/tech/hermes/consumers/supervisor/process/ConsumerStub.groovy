package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets
import pl.allegro.tech.hermes.consumers.consumer.Consumer
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset

class ConsumerStub implements Consumer {

    int initializationCount

    int tearDownCount

    boolean consumptionStarted

    Subscription subscription

    boolean updated = false

    boolean healthy = true

    boolean wasInterrupted = false

    boolean blockOnTeardown = false

    ConsumerStub(Subscription subscription) {
        this.subscription = subscription
    }

    @Override
    void consume(Runnable signalsInterrupt) {
        consumptionStarted = true
        if (healthy) {
            signalsInterrupt.run()
        }
    }

    @Override
    void initialize() {
        initializationCount++
    }

    @Override
    void tearDown() {
        tearDownCount++
        while (blockOnTeardown) {
            try {
                Thread.sleep(50)
            } catch (InterruptedException ex) {
                wasInterrupted = true
            }
        }
    }

    @Override
    void updateSubscription(Subscription subscription) {
        this.subscription = subscription
        this.updated = true
    }

    @Override
    Subscription getSubscription() {
        return subscription
    }

    @Override
    Set<Integer> getAssignedPartitions() {
        return null
    }

    @Override
    void updateTopic(Topic topic) {
    }

    @Override
    void commit(Set<SubscriptionPartitionOffset> offsets) {
    }

    @Override
    PartitionOffsets moveOffset(PartitionOffsets partitionOffset) {
        return partitionOffset
    }

    boolean getInitialized() {
        return initializationCount > 0
    }

    boolean getTearDown() {
        return tearDownCount.intValue() > 0
    }

    boolean getUpdated() {
        return updated
    }

    void whenUnhealthy(Closure closure) {
        try {
            healthy = false
            closure.run()
        } finally {
            healthy = true
        }
    }

    void whenBlockedOnTeardown(Closure closure) {
        try {
            blockOnTeardown = true
            closure.run()
        } finally {
            blockOnTeardown = false
        }
    }
}
