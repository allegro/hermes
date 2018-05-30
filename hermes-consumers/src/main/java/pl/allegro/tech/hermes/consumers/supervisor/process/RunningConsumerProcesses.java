package pl.allegro.tech.hermes.consumers.supervisor.process;

import com.google.common.collect.ImmutableSet;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class RunningConsumerProcesses {

    private final Map<SubscriptionName, RunningProcess> processes = new ConcurrentHashMap<>();

    void add(ConsumerProcess process, Future executionHandle) {
        this.processes.put(process.getSubscription().getQualifiedName(), new RunningProcess(process, executionHandle));
    }

    void remove(SubscriptionName subscriptionName) {
        processes.remove(subscriptionName);
    }

    Future getExecutionHandle(SubscriptionName subscriptionName) {
        return processes.get(subscriptionName).getExecutionHandle();
    }

    ConsumerProcess getProcess(SubscriptionName subscriptionName) {
        return processes.get(subscriptionName).process;
    }

    boolean hasProcess(SubscriptionName subscriptionName) {
        return processes.containsKey(subscriptionName);
    }

    Stream<ConsumerProcess> stream() {
        return processes.values().stream().map(p -> p.process);
    }

    Set<SubscriptionName> existingConsumers() {
        return ImmutableSet.copyOf(processes.keySet());
    }

    List<RunningSubscriptionStatus> listRunningSubscriptions() {
        return processes.entrySet().stream()
                .map(entry -> new RunningSubscriptionStatus(
                        entry.getKey().getQualifiedName(),
                        entry.getValue().getProcess().getSignalTimesheet()))
                .sorted((s1, s2) -> String.CASE_INSENSITIVE_ORDER.compare(s1.getQualifiedName(), s2.getQualifiedName()))
                .collect(toList());
    }

    Integer count() {
        return processes.size();
    }

    private static class RunningProcess {
        ConsumerProcess process;

        Future executionHandle;

        public RunningProcess(ConsumerProcess process, Future executionHandle) {
            this.process = process;
            this.executionHandle = executionHandle;
        }

        public ConsumerProcess getProcess() {
            return process;
        }

        public Future getExecutionHandle() {
            return executionHandle;
        }
    }

}
