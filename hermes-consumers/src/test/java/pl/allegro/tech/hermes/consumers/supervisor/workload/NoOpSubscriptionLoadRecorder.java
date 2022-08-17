package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;

public class NoOpSubscriptionLoadRecorder implements SubscriptionLoadRecorder {

    @Override
    public void initialize() {

    }

    @Override
    public void recordSingleOperation() {

    }

    @Override
    public void shutdown() {

    }
}
