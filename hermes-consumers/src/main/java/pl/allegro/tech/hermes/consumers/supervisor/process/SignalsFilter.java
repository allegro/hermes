package pl.allegro.tech.hermes.consumers.supervisor.process;

import com.google.common.collect.ImmutableMap;
import pl.allegro.tech.hermes.consumers.queue.MpscQueue;
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SignalsFilter {

    private static final Map<SignalType, SignalType> MERGEABLE_SIGNALS = ImmutableMap.<SignalType, SignalType>builder()
            .put(SignalType.START, SignalType.STOP)
            .put(SignalType.STOP, SignalType.START)
            .build();

    private final Clock clock;

    private final MpscQueue<Signal> taskQueue;

    SignalsFilter(MpscQueue<Signal> taskQueue, Clock clock) {
        this.taskQueue = taskQueue;
        this.clock = clock;
    }

    List<Signal> filterSignals(List<Signal> signals) {
        List<Signal> filteredSignals = new ArrayList<>(signals.size());

        for (Signal signal : signals) {
            boolean merged = merge(filteredSignals, signal);
            if (!merged) {
                if (signal.canExecuteNow(clock.millis())) {
                    filteredSignals.add(signal);
                } else {
                    taskQueue.offer(signal);
                }
            }
        }

        return filteredSignals;
    }

    private boolean merge(List<Signal> filteredSignals, Signal signal) {
        SignalType signalTypeToMerge = MERGEABLE_SIGNALS.get(signal.getType());
        if (signalTypeToMerge != null) {
            return filteredSignals.remove(signal.createChild(signalTypeToMerge));
        }
        return false;
    }
}
