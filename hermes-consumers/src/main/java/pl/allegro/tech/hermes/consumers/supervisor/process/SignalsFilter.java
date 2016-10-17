package pl.allegro.tech.hermes.consumers.supervisor.process;

import com.google.common.collect.ImmutableMap;
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

    private static final Map<SignalType, SignalType> OVERRULING_SIGNALS = ImmutableMap.<SignalType, SignalType>builder()
            .put(SignalType.START, SignalType.KILL_UNHEALTHY)
            .build();

    private final Clock clock;

    private final MonitoredMpscQueue<Signal> taskQueue;

    SignalsFilter(MonitoredMpscQueue<Signal> taskQueue, Clock clock) {
        this.taskQueue = taskQueue;
        this.clock = clock;
    }

    Set<Signal> filterSignals(List<Signal> signals, Set<SubscriptionName> exisitingConsumers) {
        Set<Signal> filteredSignals = Collections.newSetFromMap(new LinkedHashMap<>(signals.size()));

        for (Signal signal : signals) {
            boolean merged = merge(filteredSignals, signal);
            if (!merged) {
                negate(filteredSignals, signal);

                if (signal.getType() == SignalType.START || exisitingConsumers.contains(signal.getTarget())) {
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
            return filteredSignals.remove(equalSignal(signalTypeToMerge, signal.getTarget()));
        }
        return false;
    }

    private void negate(Set<Signal> filteredSignals, Signal signal) {
        SignalType signalToRemove = OVERRULING_SIGNALS.get(signal.getType());
        if (signalToRemove != null) {
            filteredSignals.remove(equalSignal(signalToRemove, signal.getTarget()));
        }
    }

    private Signal equalSignal(SignalType type, SubscriptionName target) {
        return Signal.of(type, target);
    }
}
