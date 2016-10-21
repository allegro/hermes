package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProvider;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProviderFactory;

import javax.inject.Inject;

public class OutputRateCalculatorFactory {

    private final ConfigFactory configFactory;
    private final MaxRateProviderFactory maxRateProviderFactory;

    @Inject
    public OutputRateCalculatorFactory(ConfigFactory configFactory,
                                       MaxRateProviderFactory maxRateProviderFactory) {
        this.configFactory = configFactory;
        this.maxRateProviderFactory = maxRateProviderFactory;
    }

    public OutputRateCalculator createCalculator(Subscription subscription, SendCounters sendCounters) {
        MaxRateProvider maxRateProvider =
                maxRateProviderFactory.create(subscription, sendCounters);
        return new OutputRateCalculator(configFactory, maxRateProvider);
    }
}
