package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProvider;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProviderFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateSupervisor;

import javax.inject.Inject;

public class OutputRateCalculatorFactory {

    private final ConfigFactory configFactory;
    private final MaxRateSupervisor maxRateSupervisor;
    private final MaxRateProviderFactory maxRateProviderFactory;

    @Inject
    public OutputRateCalculatorFactory(ConfigFactory configFactory,
                                       MaxRateSupervisor maxRateSupervisor,
                                       MaxRateProviderFactory maxRateProviderFactory) {
        this.configFactory = configFactory;
        this.maxRateSupervisor = maxRateSupervisor;
        this.maxRateProviderFactory = maxRateProviderFactory;
    }

    public OutputRateCalculator createCalculator(Subscription subscription, SendCounters sendCounters) {
        MaxRateProvider maxRateProvider = maxRateProviderFactory.create(subscription, sendCounters);
        maxRateSupervisor.register(maxRateProvider);
        return new OutputRateCalculator(subscription, configFactory, maxRateProvider);
    }
}
