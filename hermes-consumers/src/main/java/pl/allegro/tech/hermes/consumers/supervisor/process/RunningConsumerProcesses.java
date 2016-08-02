package pl.allegro.tech.hermes.consumers.supervisor.process;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Stream;

class RunningConsumerProcesses {

    private final Map<SubscriptionName, RunningProcess> processes = new HashMap<>();

    void add(ConsumerProcess process, Future executionHandle) {
        this.processes.put(process.getSubscriptionName(), new RunningProcess(process, executionHandle));
    }

    void remove(ConsumerProcess process) {
        processes.remove(process.getSubscriptionName());
    }

    Future getExecutionHandle(ConsumerProcess process) {
        return processes.get(process.getSubscriptionName()).executionHandle;
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

    private static class RunningProcess {
        ConsumerProcess process;

        Future executionHandle;

        public RunningProcess(ConsumerProcess process, Future executionHandle) {
            this.process = process;
            this.executionHandle = executionHandle;
        }
    }

}
