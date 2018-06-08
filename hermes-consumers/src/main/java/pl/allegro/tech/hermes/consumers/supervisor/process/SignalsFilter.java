package pl.allegro.tech.hermes.consumers.supervisor.process;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType;

import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SignalsFilter {

    private static final Map<SignalType, SignalType> MERGEABLE_SIGNALS = ImmutableMap.<SignalType, SignalType>builder()
            .put(SignalType.START, SignalType.STOP)
            .put(SignalType.STOP, SignalType.START)
            .build();

    private static final Set<SignalType> SIGNALS_PROCESSABLE_ON_DYING_PROCESSES = ImmutableSet.<SignalType>builder()
            .add(SignalType.CLEANUP)
            .add(SignalType.FORCE_KILL_DYING)
            .build();

    private static final Set<SignalType> SIGNALS_PROCESSABLE_ON_RUNNING_PROCESSES = ImmutableSet.<SignalType>builder()
            .add(SignalType.COMMIT)
            .add(SignalType.START)
            .add(SignalType.STOP)
            .add(SignalType.KILL)
            .add(SignalType.RESTART)
            .add(SignalType.RESTART_UNHEALTHY)
            .add(SignalType.STOP_RESTART)
            .add(SignalType.RETRANSMIT)
            .add(SignalType.UPDATE_SUBSCRIPTION)
            .add(SignalType.UPDATE_TOPIC)
            .build();

    private final Clock clock;

    private final MonitoredMpscQueue<Signal> taskQueue;

    SignalsFilter(MonitoredMpscQueue<Signal> taskQueue, Clock clock) {
        this.taskQueue = taskQueue;
        this.clock = clock;
    }

    Set<Signal> filterSignals(List<Signal> signals, Set<SubscriptionName> existingConsumers,
                              Set<SubscriptionName> dyingConsumers) {
        Set<Signal> filteredSignals = Collections.newSetFromMap(new LinkedHashMap<>(signals.size()));

        for (Signal signal : signals) {
            boolean merged = merge(filteredSignals, signal);
            if (!merged) {
                if (signal.getType() == SignalType.START ||
                        signalCanBeScheduledForRunningProcessSubscription(signal, existingConsumers) ||
                        signalCanBeScheduledForDyingProcessSubscription(signal, dyingConsumers)) {
                    if (signal.canExecuteNow(clock.millis())) {
                        filteredSignals.add(signal);
                    } else {
                        taskQueue.offer(signal);
                    }
                }
            }
        }

        return filteredSignals;
    }

    private boolean merge(Set<Signal> filteredSignals, Signal signal) {
        SignalType signalTypeToMerge = MERGEABLE_SIGNALS.get(signal.getType());
        if (signalTypeToMerge != null) {
            return filteredSignals.remove(signal.createChild(signalTypeToMerge));
        }
        return false;
    }

    private boolean signalCanBeScheduledForRunningProcessSubscription(Signal signal,
                                                                      Set<SubscriptionName> runningConsumers) {
        return SIGNALS_PROCESSABLE_ON_RUNNING_PROCESSES.contains(signal.getType()) &&
                runningConsumers.contains(signal.getTarget());
    }

    private boolean signalCanBeScheduledForDyingProcessSubscription(Signal signal,
                                                                    Set<SubscriptionName> dyingConsumers) {
        return SIGNALS_PROCESSABLE_ON_DYING_PROCESSES.contains(signal.getType()) &&
                dyingConsumers.contains(signal.getTarget());
    }

}
