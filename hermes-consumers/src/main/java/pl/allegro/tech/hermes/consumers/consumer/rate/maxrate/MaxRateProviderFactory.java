package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_MAXRATE_HISTORY_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;

public class MaxRateProviderFactory {

    private final ConfigFactory configFactory;
    private final MaxRateRegistry maxRateRegistry;

    @Inject
    public MaxRateProviderFactory(ConfigFactory configFactory, MaxRateRegistry maxRateRegistry) {
        this.configFactory = configFactory;
        this.maxRateRegistry = maxRateRegistry;
    }

    public MaxRateProvider create(Subscription subscription, SendCounters sendCounters) {
        String consumerId = configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID);
        int historyLimit = configFactory.getIntProperty(CONSUMER_MAXRATE_HISTORY_SIZE);
        return new MaxRateProvider(consumerId, maxRateRegistry, subscription, sendCounters, historyLimit);
    }
}
