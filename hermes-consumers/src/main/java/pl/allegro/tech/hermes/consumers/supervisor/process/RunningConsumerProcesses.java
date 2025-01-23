package pl.allegro.tech.hermes.consumers.supervisor.process;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.SubscriptionName;

class RunningConsumerProcesses {

  private final Map<SubscriptionName, RunningConsumerProcess> processes = new ConcurrentHashMap<>();
  private final Clock clock;

  RunningConsumerProcesses(Clock clock) {
    this.clock = clock;
  }

  void add(ConsumerProcess process, Future executionHandle) {
    this.processes.put(
        process.getSubscription().getQualifiedName(),
        new RunningConsumerProcess(process, executionHandle, clock));
  }

  void add(RunningConsumerProcess process) {
    this.processes.put(process.getConsumerProcess().getSubscriptionName(), process);
  }

  void remove(SubscriptionName subscriptionName) {
    processes.remove(subscriptionName);
  }

  public void remove(ConsumerProcess consumerProcess) {
    remove(consumerProcess.getSubscriptionName());
  }

  void remove(RunningConsumerProcess runningProcess) {
    remove(runningProcess.getConsumerProcess().getSubscriptionName());
  }

  RunningConsumerProcess getProcess(SubscriptionName subscriptionName) {
    return processes.get(subscriptionName);
  }

  boolean hasProcess(SubscriptionName subscriptionName) {
    return processes.containsKey(subscriptionName);
  }

  Stream<RunningConsumerProcess> stream() {
    return processes.values().stream();
  }

  Set<SubscriptionName> existingConsumers() {
    return ImmutableSet.copyOf(processes.keySet());
  }

  List<RunningSubscriptionStatus> listRunningSubscriptions() {
    return processes.entrySet().stream()
        .map(
            entry ->
                new RunningSubscriptionStatus(
                    entry.getKey().getQualifiedName(),
                    entry.getValue().getConsumerProcess().getSignalTimesheet()))
        .sorted(
            (s1, s2) ->
                String.CASE_INSENSITIVE_ORDER.compare(s1.getQualifiedName(), s2.getQualifiedName()))
        .collect(toList());
  }

  Integer count() {
    return processes.size();
  }
}
