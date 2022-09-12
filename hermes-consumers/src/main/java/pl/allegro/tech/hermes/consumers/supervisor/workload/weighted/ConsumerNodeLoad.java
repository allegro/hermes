package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;
import java.util.Objects;

class ConsumerNodeLoad {

    static final ConsumerNodeLoad UNDEFINED = new ConsumerNodeLoad(-1d, Map.of());

    private final double cpuUtilization;
    private final Map<SubscriptionName, SubscriptionLoad> loadPerSubscription;

    ConsumerNodeLoad(double cpuUtilization, Map<SubscriptionName, SubscriptionLoad> loadPerSubscription) {
        this.cpuUtilization = cpuUtilization;
        this.loadPerSubscription = loadPerSubscription;
    }

    Map<SubscriptionName, SubscriptionLoad> getLoadPerSubscription() {
        return loadPerSubscription;
    }

    double getCpuUtilization() {
        return cpuUtilization;
    }

    double sumOperationsPerSecond() {
        return loadPerSubscription.values().stream()
                .mapToDouble(SubscriptionLoad::getOperationsPerSecond)
                .sum();
    }

    boolean isDefined() {
        return cpuUtilization != UNDEFINED.getCpuUtilization();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsumerNodeLoad that = (ConsumerNodeLoad) o;
        return Objects.equals(loadPerSubscription, that.loadPerSubscription)
                && Double.compare(cpuUtilization, that.cpuUtilization) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpuUtilization, loadPerSubscription);
    }
}
