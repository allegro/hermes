package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ConsumerWorkloadRegistryType {

    HIERARCHICAL("hierarchical"),
    FLAT_BINARY("flat-binary");

    private final String configValue;

    ConsumerWorkloadRegistryType(String configValue) {
        this.configValue = configValue;
    }

    public static ConsumerWorkloadRegistryType fromString(String value) {
        switch (value) {
            case "hierarchical":
                return HIERARCHICAL;
            case "flat-binary":
                return FLAT_BINARY;
            default:
                throw new UnknownSubscriptionAssignmentRegistryTypeException(value);
        }
    }

    public String getConfigValue() {
        return configValue;
    }

    public static class UnknownSubscriptionAssignmentRegistryTypeException extends IllegalArgumentException {
        UnknownSubscriptionAssignmentRegistryTypeException(String value) {
            super(String.format("Unknown consumer workload registry type: %s. Use one of: %s", value,
                    Arrays.stream(ConsumerWorkloadRegistryType.values())
                            .map(ConsumerWorkloadRegistryType::getConfigValue)
                            .collect(Collectors.joining(", "))));
        }
    }
}
