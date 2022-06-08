package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.google.common.base.Preconditions;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

public class MaxRateProviderFactory {

    private final Creator providerCreator;

    public MaxRateProviderFactory(MaxRateParameters maxRateParameters,
                                  String nodeId,
                                  MaxRateRegistry maxRateRegistry,
                                  MaxRateSupervisor maxRateSupervisor,
                                  HermesMetrics metrics) {

        checkNegotiatedSettings(maxRateParameters.getMinSignificantUpdatePercent() / 100, maxRateParameters.getBusyTolerance());
        providerCreator = (subscription, sendCounters) -> {
            int historyLimit = maxRateParameters.getHistorySize();
            double initialMaxRate = maxRateParameters.getMinMaxRate();
            double minSignificantChange =
                    maxRateParameters.getMinSignificantUpdatePercent() / 100;

            return new NegotiatedMaxRateProvider(nodeId, maxRateRegistry, maxRateSupervisor,
                    subscription, sendCounters, metrics, initialMaxRate, minSignificantChange, historyLimit);
        };
    }

    public MaxRateProvider create(Subscription subscription, SendCounters sendCounters) {
        return providerCreator.create(subscription, sendCounters);
    }

    private interface Creator {
        MaxRateProvider create(Subscription subscription, SendCounters sendCounters);
    }

    private void checkNegotiatedSettings(double minSignificantChange, double busyTolerance) {
        Preconditions.checkArgument(busyTolerance > minSignificantChange,
                "Significant rate change (%s) can't be higher than busy tolerance (%s)",
                minSignificantChange, busyTolerance);
    }
}
