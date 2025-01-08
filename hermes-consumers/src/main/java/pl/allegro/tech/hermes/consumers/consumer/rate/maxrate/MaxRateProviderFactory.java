package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.google.common.base.Preconditions;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

public class MaxRateProviderFactory {

  private final Creator providerCreator;

  public MaxRateProviderFactory(
      MaxRateParameters maxRateParameters,
      String nodeId,
      MaxRateRegistry maxRateRegistry,
      MaxRateSupervisor maxRateSupervisor) {
    double minSignificantChange = maxRateParameters.getMinSignificantUpdatePercent() / 100;
    checkNegotiatedSettings(minSignificantChange, maxRateParameters.getBusyTolerance());
    providerCreator =
        (subscription, sendCounters, metrics) -> {
          int historyLimit = maxRateParameters.getHistorySize();
          double initialMaxRate = maxRateParameters.getMinMaxRate();

          return new NegotiatedMaxRateProvider(
              nodeId,
              maxRateRegistry,
              maxRateSupervisor,
              subscription,
              sendCounters,
              metrics,
              initialMaxRate,
              minSignificantChange,
              historyLimit);
        };
  }

  public MaxRateProvider create(
      Subscription subscription, SendCounters sendCounters, MetricsFacade metrics) {
    return providerCreator.create(subscription, sendCounters, metrics);
  }

  private interface Creator {
    MaxRateProvider create(
        Subscription subscription, SendCounters sendCounters, MetricsFacade metrics);
  }

  private void checkNegotiatedSettings(double minSignificantChange, double busyTolerance) {
    Preconditions.checkArgument(
        busyTolerance > minSignificantChange,
        "Significant rate change (%s) can't be higher than busy tolerance (%s)",
        minSignificantChange,
        busyTolerance);
  }
}
