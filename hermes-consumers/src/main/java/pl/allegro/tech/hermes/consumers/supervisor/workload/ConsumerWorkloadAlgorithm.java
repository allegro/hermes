package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import static java.lang.String.join;

public final class ConsumerWorkloadAlgorithm {
    public static final String LEGACY_MIRROR = "legacy.mirror";
    public static final String MIRROR = "mirror";
    public static final String SELECTIVE = "selective";

    public static class UnsupportedConsumerWorkloadAlgorithm extends InternalProcessingException {
        public UnsupportedConsumerWorkloadAlgorithm() {
            super("Unsupported algorithm. Supported: " + join(",", LEGACY_MIRROR, MIRROR, SELECTIVE));
        }
    }
}
