package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.ActiveConsumerCounter;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_MAXRATE_HISTORY_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_MAXRATE_MIN_MAX_RATE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_MAXRATE_STRATEGY;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;
import static pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.ConsumerMaxRateStrategy.NEGOTIATED;
import static pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.ConsumerMaxRateStrategy.STRICT;

public class MaxRateProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(MaxRateProviderFactory.class);

    private final Creator providerCreator;

    @Inject
    public MaxRateProviderFactory(ConfigFactory configFactory,
                                  MaxRateRegistry maxRateRegistry,
                                  MaxRateSupervisor maxRateSupervisor,
                                  ActiveConsumerCounter activeConsumerCounter,
                                  HermesMetrics metrics) {

        String strategy = configFactory.getStringProperty(CONSUMER_MAXRATE_STRATEGY);
        logger.info("Max rate provider strategy chosen: {}", strategy);

        switch (strategy) {
            case NEGOTIATED:
                providerCreator = (subscription, sendCounters) -> {
                    String consumerId = configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID);
                    int historyLimit = configFactory.getIntProperty(CONSUMER_MAXRATE_HISTORY_SIZE);
                    double fallbackMaxRate = configFactory.getIntProperty(CONSUMER_MAXRATE_MIN_MAX_RATE);

                    return new NegotiatedMaxRateProvider(consumerId, maxRateRegistry, maxRateSupervisor,
                            subscription, sendCounters, metrics, fallbackMaxRate, historyLimit);
                };
                break;
            case STRICT:
                providerCreator = (subscription, sendCounters) ->
                        new StrictMaxRateProvider(activeConsumerCounter, subscription);
                break;
            default:
                throw new ConsumerMaxRateStrategy.UnknownMaxRateStrategyException();
        }
    }

    public MaxRateProvider create(Subscription subscription, SendCounters sendCounters) {
        return providerCreator.create(subscription, sendCounters);
    }

    private interface Creator {
        MaxRateProvider create(Subscription subscription, SendCounters sendCounters);
    }
}
