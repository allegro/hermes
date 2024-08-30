package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProvider;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProviderFactory;

public class OutputRateCalculatorFactory {

  private final RateCalculatorParameters rateCalculatorParameters;
  private final MaxRateProviderFactory maxRateProviderFactory;

  public OutputRateCalculatorFactory(
      RateCalculatorParameters rateCalculatorParameters,
      MaxRateProviderFactory maxRateProviderFactory) {
    this.rateCalculatorParameters = rateCalculatorParameters;
    this.maxRateProviderFactory = maxRateProviderFactory;
  }

  public OutputRateCalculator createCalculator(
      Subscription subscription, SendCounters sendCounters, MetricsFacade metrics) {
    MaxRateProvider maxRateProvider =
        maxRateProviderFactory.create(subscription, sendCounters, metrics);
    return new OutputRateCalculator(rateCalculatorParameters, maxRateProvider);
  }
}
