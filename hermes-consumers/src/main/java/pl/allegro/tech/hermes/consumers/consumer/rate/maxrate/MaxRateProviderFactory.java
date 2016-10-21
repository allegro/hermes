package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import avro.shaded.com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.ActiveConsumerCounter;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_MAXRATE_HISTORY_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_MAXRATE_STRATEGY;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;
import static pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.ConsumerMaxRateStrategy.NEGOTIATED;
import static pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.ConsumerMaxRateStrategy.STRICT;

public class MaxRateProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(MaxRateProviderFactory.class);

    private final ConfigFactory configFactory;
    private final Map<String, Creator> providerStrategies;

    @Inject
    public MaxRateProviderFactory(ConfigFactory configFactory,
                                  MaxRateRegistry maxRateRegistry,
                                  MaxRateSupervisor maxRateSupervisor,
                                  ActiveConsumerCounter activeConsumerCounter,
                                  HermesMetrics metrics) {
        this.configFactory = configFactory;
        this.providerStrategies = ImmutableMap.of(
                NEGOTIATED, (subscription, sendCounters) -> {
                    String consumerId = configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID);
                    int historyLimit = configFactory.getIntProperty(CONSUMER_MAXRATE_HISTORY_SIZE);
                    return new NegotiatedMaxRateProvider(consumerId,
                            maxRateRegistry, maxRateSupervisor, subscription, sendCounters, metrics, historyLimit);
                },
                STRICT, (subscription, sendCounters) -> new StrictMaxRateProvider(activeConsumerCounter, subscription));
    }

    public MaxRateProvider create(Subscription subscription, SendCounters sendCounters) {
        String strategy = configFactory.getStringProperty(CONSUMER_MAXRATE_STRATEGY);
        // TODO we can log this elsewhere
        logger.info("Max rate provider strategy chosen: {}", strategy);
        return Optional.ofNullable(providerStrategies.get(strategy))
                .map(provider -> provider.create(subscription, sendCounters))
                .orElseThrow(ConsumerMaxRateStrategy.UnknownMaxRateStrategyException::new);
    }

    private interface Creator {
        MaxRateProvider create(Subscription subscription, SendCounters sendCounters);
    }
}
