package pl.allegro.tech.hermes.consumers.supervisor.process;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class RunningConsumerProcesses {

    private final Map<SubscriptionName, RunningProcess> processes = new ConcurrentHashMap<>();

    void add(ConsumerProcess process, Future executionHandle) {
        this.processes.put(process.getSubscriptionName(), new RunningProcess(process, executionHandle));
    }

    void remove(SubscriptionName subscriptionName) {
        processes.remove(subscriptionName);
    }

    void remove(ConsumerProcess process) {
        processes.remove(process.getSubscriptionName());
    }

    Future getExecutionHandle(SubscriptionName subscriptionName) {
        return processes.get(subscriptionName).executionHandle;
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
        return processes.keySet();
    }

    public List<String> listRunningSubscriptions() {
        return processes.keySet().stream()
                .map(SubscriptionName::getQualifiedName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(toList());
    }

    public Integer count() {
        return processes.size();
    }

    private static class RunningProcess {
        ConsumerProcess process;

        Future executionHandle;

        public RunningProcess(ConsumerProcess process, Future executionHandle) {
            this.process = process;
            this.executionHandle = executionHandle;
        }
    }

}
