package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MaxRateBalancer {

  private static final double ALLOWED_DISTRIBUTION_ERROR = 1.0d;

  private final double busyTolerance;
  private final double minMax;
  private final double minAllowedChangePercent;

  MaxRateBalancer(double busyTolerance, double minMax, double minAllowedChangePercent) {
    this.busyTolerance = busyTolerance;
    this.minMax = minMax;
    this.minAllowedChangePercent = minAllowedChangePercent;
  }

  Optional<Map<String, MaxRate>> balance(double subscriptionMax, Set<ConsumerRateInfo> rateInfos) {
    double defaultRate = Math.max(minMax, subscriptionMax / Math.max(1, rateInfos.size()));

    if (shouldResortToDefaults(subscriptionMax, rateInfos)) {
      return Optional.of(balanceDefault(defaultRate, rateInfos));
    }

    List<ActiveConsumerInfo> activeConsumerInfos =
        rateInfos.stream().map(ActiveConsumerInfo::convert).collect(Collectors.toList());

    if (subscriptionRateChanged(activeConsumerInfos, subscriptionMax)) {
      return Optional.of(balanceDefault(defaultRate, rateInfos));
    }

    Map<Boolean, List<ActiveConsumerInfo>> busyOrNot = busyOrNot(activeConsumerInfos);
    List<ActiveConsumerInfo> busy = busyOrNot.get(true);
    List<ActiveConsumerInfo> notBusy =
        busyOrNot.get(false).stream()
            .filter(consumer -> !consumer.getRateHistory().getRates().isEmpty())
            .collect(Collectors.toList());

    if (busy.isEmpty()) {
      return Optional.empty();
    }

    double minChange = (minAllowedChangePercent / 100) * subscriptionMax;
    NotBusyBalancer.Result notBusyChanges = handleNotBusy(notBusy, minChange);
    Map<String, MaxRate> busyUpdates =
        handleBusy(minChange, busy, notBusyChanges.getReleasedRate()).calculateNewMaxRates();
    Map<String, MaxRate> notBusyUpdates = notBusyChanges.calculateNewMaxRates();

    return Optional.of(
        Stream.of(busyUpdates, notBusyUpdates)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  private boolean shouldResortToDefaults(double subscriptionMax, Set<ConsumerRateInfo> rateInfos) {
    return anyNewConsumers(rateInfos)
        || insufficientSubscriptionRate(subscriptionMax, rateInfos.size());
  }

  private Map<String, MaxRate> balanceDefault(double defaultRate, Set<ConsumerRateInfo> rateInfos) {
    return rateInfos.stream()
        .collect(
            Collectors.toMap(
                ConsumerRateInfo::getConsumerId, rateInfo -> new MaxRate(defaultRate)));
  }

  private boolean anyNewConsumers(Set<ConsumerRateInfo> rateInfos) {
    return rateInfos.stream().anyMatch(this::isUnassigned);
  }

  private boolean insufficientSubscriptionRate(double subscriptionMax, int consumersCount) {
    return subscriptionMax / consumersCount <= 1.0d;
  }

  private boolean subscriptionRateChanged(
      List<ActiveConsumerInfo> activeConsumerInfos, double subscriptionMax) {
    double sum = activeConsumerInfos.stream().mapToDouble(ActiveConsumerInfo::getMax).sum();
    return Math.abs(sum - subscriptionMax) > ALLOWED_DISTRIBUTION_ERROR;
  }

  private boolean isUnassigned(ConsumerRateInfo rateInfo) {
    return !rateInfo.getMaxRate().isPresent();
  }

  private Map<Boolean, List<ActiveConsumerInfo>> busyOrNot(List<ActiveConsumerInfo> infos) {
    return infos.stream().collect(Collectors.partitioningBy(this::isBusy));
  }

  private boolean isBusy(ActiveConsumerInfo info) {
    return info.getRateHistory().getRates().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0)
        > 1.0 - busyTolerance;
  }

  private NotBusyBalancer.Result handleNotBusy(List<ActiveConsumerInfo> notBusy, double minChange) {
    return new NotBusyBalancer(notBusy, minChange, minMax).balance();
  }

  private BusyBalancer.Result handleBusy(
      double minChange, List<ActiveConsumerInfo> busy, double freedByNotBusy) {
    return new BusyBalancer(busy, freedByNotBusy, minChange).balance();
  }

  private static class ActiveConsumerInfo {

    private final String consumerId;
    private final RateHistory rateHistory;
    private final Double max;

    ActiveConsumerInfo(String consumerId, RateHistory rateHistory, Double max) {
      this.consumerId = consumerId;
      this.rateHistory = rateHistory;
      this.max = max;
    }

    static ActiveConsumerInfo convert(ConsumerRateInfo rateInfo) {
      return new ActiveConsumerInfo(
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

    private final List<ActiveConsumerInfo> consumerInfos;
    private final double minChange;
    private final double minMax;

    NotBusyBalancer(List<ActiveConsumerInfo> consumerInfos, double minChange, double minMax) {
      this.consumerInfos = consumerInfos;
      this.minChange = minChange;
      this.minMax = minMax;
    }

    Result balance() {
      List<ConsumerRateChange> changes =
          consumerInfos.stream()
              .map(
                  ri -> {
                    double currentMax = ri.getMax();
                    double toDistribute = takeAwayFromNotBusy(ri.getRateHistory(), currentMax);
                    return new ConsumerRateChange(ri.getConsumerId(), currentMax, -toDistribute);
                  })
              .collect(Collectors.toList());

      return new Result(changes);
    }

    private double takeAwayFromNotBusy(RateHistory history, double currentMax) {
      double usedRatio = history.getRates().get(0); // rate history must be present
      double scalingFactor = 2 / (usedRatio + 1.0) - 1;

      double proposedChange = scalingFactor * currentMax;
      double actualChange = proposedChange > minChange ? proposedChange : 0.0d;

      return currentMax - actualChange > minMax ? actualChange : currentMax - minMax;
    }

    private static class Result {

      private final List<ConsumerRateChange> changes;
      private final double releasedRate;

      Result(List<ConsumerRateChange> changes) {
        this.changes = changes;
        this.releasedRate = -changes.stream().mapToDouble(ConsumerRateChange::getRateChange).sum();
      }

      double getReleasedRate() {
        return releasedRate;
      }

      Map<String, MaxRate> calculateNewMaxRates() {
        return changes.stream()
            .collect(
                Collectors.toMap(
                    ConsumerRateChange::getConsumerId,
                    change -> new MaxRate(change.getCurrentMax() + change.getRateChange())));
      }
    }
  }

  private static class BusyBalancer {

    private final List<ActiveConsumerInfo> consumerInfos;
    private final double freedByNotBusy;
    private final double minChange;

    BusyBalancer(List<ActiveConsumerInfo> consumerInfos, double freedByNotBusy, double minChange) {
      this.consumerInfos = consumerInfos;
      this.freedByNotBusy = freedByNotBusy;
      this.minChange = minChange;
    }

    Result balance() {
      double busyMaxSum = consumerInfos.stream().mapToDouble(ActiveConsumerInfo::getMax).sum();

      List<ConsumerMaxShare> shares =
          consumerInfos.stream()
              .map(
                  info ->
                      new ConsumerMaxShare(
                          info.getConsumerId(), info.getMax(), info.getMax() / busyMaxSum))
              .collect(Collectors.toList());

      if (shares.size() == 1) {
        return new Result(distribute(freedByNotBusy, shares));
      }

      double equalShare = busyMaxSum / consumerInfos.size();

      Map<Boolean, List<ConsumerMaxShare>> greedyOrNot =
          shares.stream()
              .collect(
                  Collectors.partitioningBy(
                      share -> share.getCurrentMax() > equalShare + minChange));

      List<ConsumerMaxShare> greedy = greedyOrNot.get(true);
      List<ConsumerMaxShare> notGreedy = greedyOrNot.get(false);

      List<ConsumerRateChange> greedySubtracts =
          greedy.stream()
              .map(
                  share -> {
                    double toDistribute =
                        takeAwayFromGreedy(share.getCurrentMax(), share.getShare(), equalShare);
                    return new ConsumerRateChange(
                        share.getConsumerId(), share.currentMax, -toDistribute);
                  })
              .collect(Collectors.toList());

      double toDistribute =
          freedByNotBusy
              - greedySubtracts.stream().mapToDouble(ConsumerRateChange::getRateChange).sum();

      List<ConsumerMaxShare> notGreedyShares = recalculateShare(notGreedy);
      List<ConsumerRateChange> notGreedyAdds = distribute(toDistribute, notGreedyShares);

      return new Result(
          Stream.concat(notGreedyAdds.stream(), greedySubtracts.stream())
              .collect(Collectors.toList()));
    }

    private double takeAwayFromGreedy(double currentMax, double share, double equalShare) {
      double scale = 2 / (share + 1.0) - 1;
      double changeProposal = (currentMax / 2) * scale;
      double actualChange = Math.max(changeProposal, minChange);
      return (currentMax - actualChange) > equalShare ? actualChange : currentMax - equalShare;
    }

    private List<ConsumerMaxShare> recalculateShare(List<ConsumerMaxShare> shares) {
      double sum = shares.stream().mapToDouble(ConsumerMaxShare::getCurrentMax).sum();
      return shares.stream()
          .map(
              previous ->
                  new ConsumerMaxShare(
                      previous.getConsumerId(),
                      previous.getCurrentMax(),
                      previous.currentMax / sum))
          .collect(Collectors.toList());
    }

    private List<ConsumerRateChange> distribute(double maxAmount, List<ConsumerMaxShare> shares) {
      return shares.stream()
          .map(
              share ->
                  new ConsumerRateChange(
                      share.consumerId, share.getCurrentMax(), share.getShare() * maxAmount))
          .collect(Collectors.toList());
    }

    private static class Result {

      private final List<ConsumerRateChange> changes;

      Result(List<ConsumerRateChange> changes) {
        this.changes = changes;
      }

      Map<String, MaxRate> calculateNewMaxRates() {
        return changes.stream()
            .collect(
                Collectors.toMap(
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
