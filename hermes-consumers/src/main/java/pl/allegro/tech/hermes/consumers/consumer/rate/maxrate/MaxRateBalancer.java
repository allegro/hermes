package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MaxRateBalancer {

    private final double busyTolerance;
    private final double minMax;
    private final double minAllowedChangePercent;

    public MaxRateBalancer(double busyTolerance, double minMax, double minAllowedChangePercent) {
        this.busyTolerance = busyTolerance;
        this.minMax = minMax;
        this.minAllowedChangePercent = minAllowedChangePercent;
    }

    Optional<Map<String, MaxRate>> balance(double subscriptionMax, Set<ConsumerRateInfo> rateInfos) {
        double minChange = (minAllowedChangePercent / 100) * subscriptionMax;
        double defaultRate = subscriptionMax / Math.max(1, rateInfos.size());

        if (anyNewConsumers(rateInfos)) {
            return Optional.of(balanceDefault(defaultRate, rateInfos));
        }

        List<ConsumerInfo> consumerInfos = rateInfos.stream().map(ConsumerInfo::convert).collect(Collectors.toList());

        Map<Boolean, List<ConsumerInfo>> busyOrNot = busyOrNot(consumerInfos);

        List<ConsumerInfo> notBusy = busyOrNot.get(false);
        List<ConsumerInfo> busy = busyOrNot.get(true);

        if (busy.isEmpty()) {
            return Optional.empty();
        }

        NotBusyBalancer.View notBusyChanges = handleNotBusy(notBusy, minChange);
        Map<String, MaxRate> busyUpdates = handleBusy(minChange, busy, notBusyChanges.getReleasedRate()).calculateNewMaxRates();
        Map<String, MaxRate> notBusyUpdates = notBusyChanges.calculateNewMaxRates();

        return Optional.of(Stream.of(busyUpdates, notBusyUpdates)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private Map<String, MaxRate> balanceDefault(double defaultRate, Set<ConsumerRateInfo> rateInfos) {
        return rateInfos.stream()
                .collect(Collectors.toMap(ConsumerRateInfo::getConsumerId,
                        rateInfo -> new MaxRate(defaultRate)));
    }

    private boolean anyNewConsumers(Set<ConsumerRateInfo> rateInfos) {
        return rateInfos.stream().anyMatch(this::isUnassigned);
    }

    private boolean isUnassigned(ConsumerRateInfo rateInfo) {
        return !rateInfo.getMaxRate().isPresent() || rateInfo.getHistory().getRates().isEmpty();
    }

    private Map<Boolean, List<ConsumerInfo>> busyOrNot(List<ConsumerInfo> infos) {
        return infos.stream().collect(Collectors.partitioningBy(this::isBusy));
    }

    private boolean isBusy(ConsumerInfo info) {
        return info.getRateHistory().getRates().stream()
                .mapToDouble(Double::doubleValue).average().getAsDouble() > 1.0 - busyTolerance;
    }

    private NotBusyBalancer.View handleNotBusy(List<ConsumerInfo> notBusy, double minChange) {
        return new NotBusyBalancer(notBusy, minChange, minMax).balance();
    }

    private BusyBalancer.View handleBusy(double minChange, List<ConsumerInfo> busy, double freedByNotBusy) {
        return new BusyBalancer(busy, freedByNotBusy, minChange).balance();
    }

    private static class ConsumerInfo {
        private final String consumerId;
        private final RateHistory rateHistory;
        private final Double max;

        ConsumerInfo(String consumerId, RateHistory rateHistory, Double max) {
            this.consumerId = consumerId;
            this.rateHistory = rateHistory;
            this.max = max;
        }

        static ConsumerInfo convert(ConsumerRateInfo rateInfo) {
            return new ConsumerInfo(
                    rateInfo.getConsumerId(),
                    rateInfo.getHistory(),
                    rateInfo.getMaxRate().get().getMaxRate());
        }

        String getConsumerId() {
            return consumerId;
        }

        RateHistory getRateHistory() {
            return rateHistory;
        }

        Double getMax() {
            return max;
        }
    }

    private static class NotBusyBalancer {

        private final List<ConsumerInfo> infos;
        private final double minChange;
        private final double minMax;

        NotBusyBalancer(List<ConsumerInfo> infos, double minChange, double minMax) {
            this.infos = infos;
            this.minChange = minChange;
            this.minMax = minMax;
        }

        View balance() {
            List<ConsumerRateChange> changes = infos.stream().map(ri -> {
                double currentMax = ri.getMax();
                return new ConsumerRateChange(ri.getConsumerId(), currentMax, takeAwayFromNotBusy(ri.getRateHistory(), currentMax));
            }).collect(Collectors.toList());

            return new View(changes);
        }

        private double takeAwayFromNotBusy(RateHistory history, double currentMax) {
            double usedRatio = history.getRates().get(0);
            double usedRate = currentMax * usedRatio;

            double scalingFactor = 2 / (usedRatio + 1.0) - 1;

            double proposedChange = scalingFactor * usedRate;
            double actualChange = proposedChange > minChange ? -proposedChange : 0.0d;

            return currentMax - actualChange > minMax ? actualChange : minMax;
        }

        private static class View {

            private final List<ConsumerRateChange> changes;
            private final double releasedRate;

            View(List<ConsumerRateChange> changes) {
                this.changes = changes;
                releasedRate = -changes.stream().mapToDouble(ConsumerRateChange::getRateChange).sum();
            }

            double getReleasedRate() {
                return releasedRate;
            }

            Map<String, MaxRate> calculateNewMaxRates() {
                return changes.stream()
                        .collect(Collectors.toMap(
                                ConsumerRateChange::getConsumerId,
                                change -> new MaxRate(change.getCurrentMax() + change.getRateChange())));
            }
        }
    }

    private static class BusyBalancer {
        private final List<ConsumerInfo> infos;
        private final double freedByNotBusy;
        private final double minChange;

        public BusyBalancer(List<ConsumerInfo> infos, double freedByNotBusy, double minChange) {
            this.infos = infos;
            this.freedByNotBusy = freedByNotBusy;
            this.minChange = minChange;
        }

        public View balance() {
            double busyMaxSum = infos.stream().mapToDouble(ConsumerInfo::getMax).sum();

            List<ConsumerMaxShare> shares = infos.stream()
                    .map(info -> new ConsumerMaxShare(info.getConsumerId(), info.getMax(), info.getMax() / busyMaxSum))
                    .collect(Collectors.toList());

            if (shares.size() == 1) {
                return new View(distribute(freedByNotBusy, shares));
            }

            double equalShare = busyMaxSum / infos.size();

            Map<Boolean, List<ConsumerMaxShare>> greedyOrNot = shares.stream().collect(Collectors.partitioningBy(share -> share.getCurrentMax() > equalShare + minChange));

            List<ConsumerMaxShare> greedy = greedyOrNot.get(true);
            List<ConsumerMaxShare> notGreedy = greedyOrNot.get(false);

            List<ConsumerRateChange> greedySubtracts = greedy.stream().map(share -> takeAwayFromGreedy(share, equalShare)).collect(Collectors.toList());

            double toDistribute = freedByNotBusy - greedySubtracts.stream().mapToDouble(ConsumerRateChange::getRateChange).sum();

            List<ConsumerMaxShare> notGreedyShares = recalculateShare(notGreedy);

            List<ConsumerRateChange> notGreedyAdds = distribute(toDistribute, notGreedyShares);

            return new View(Stream.concat(notGreedyAdds.stream(), greedySubtracts.stream()).collect(Collectors.toList()));
        }

        private ConsumerRateChange takeAwayFromGreedy(ConsumerMaxShare consumerMaxShare, double equalShare) {
            double share = consumerMaxShare.getShare();
            double currentMax = consumerMaxShare.getCurrentMax();
            double scale = 2 / (share + 1.0) - 1;
            double changeProposal = (currentMax / 2) * scale;
            double actualChange = -Math.max(changeProposal, minChange);
            actualChange = (currentMax + actualChange) > equalShare ? actualChange : -(currentMax - equalShare);
            return new ConsumerRateChange(consumerMaxShare.getConsumerId(), currentMax, actualChange);
        }

        private List<ConsumerMaxShare> recalculateShare(List<ConsumerMaxShare> shares) {
            double sum = shares.stream().mapToDouble(ConsumerMaxShare::getCurrentMax).sum();
            return shares.stream()
                    .map(previous -> new ConsumerMaxShare(
                            previous.getConsumerId(),
                            previous.getCurrentMax(),
                            previous.currentMax / sum))
                    .collect(Collectors.toList());
        }

        private List<ConsumerRateChange> distribute(double maxAmount, List<ConsumerMaxShare> shares) {
            return shares.stream()
                    .map(share -> new ConsumerRateChange(
                            share.consumerId,
                            share.getCurrentMax(),
                            share.getShare() * maxAmount))
                    .collect(Collectors.toList());
        }

        private static class View {
            private final List<ConsumerRateChange> changes;

            View(List<ConsumerRateChange> changes) {
                this.changes = changes;
            }

            Map<String, MaxRate> calculateNewMaxRates() {
                return changes.stream()
                        .collect(Collectors.toMap(
                                ConsumerRateChange::getConsumerId,
                                change -> new MaxRate(change.getCurrentMax() + change.getRateChange())));
            }
        }

        private static class ConsumerMaxShare {
            private final String consumerId;
            private final double currentMax;
            private final double share;

            ConsumerMaxShare(String consumerId, double currentMax, double share) {
                this.consumerId = consumerId;
                this.currentMax = currentMax;
                this.share = share;
            }

            String getConsumerId() {
                return consumerId;
            }

            double getCurrentMax() {
                return currentMax;
            }

            double getShare() {
                return share;
            }
        }
    }

    private static class ConsumerRateChange {
        private String consumerId;
        private double currentMax;
        private double rateChange;

        ConsumerRateChange(String consumerId, double currentMax, double rateChange) {
            this.consumerId = consumerId;
            this.currentMax = currentMax;
            this.rateChange = rateChange;
        }

        String getConsumerId() {
            return consumerId;
        }

        double getCurrentMax() {
            return currentMax;
        }

        double getRateChange() {
            return rateChange;
        }
    }
}
