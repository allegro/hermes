package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Arrays;
import java.util.stream.Collectors;

enum ConsumerMaxRateRegistryType {

    HIERARCHICAL("hierarchical"),
    FLAT_BINARY("flat-binary");

    private final String configValue;

    ConsumerMaxRateRegistryType(String configValue) {
        this.configValue = configValue;
    }

    public static ConsumerMaxRateRegistryType fromString(String value) {
        switch (value) {
            case "hierarchical":
                return HIERARCHICAL;
            case "flat-binary":
                return FLAT_BINARY;
            default:
                throw new UnknownMaxRateRegistryTypeException(value);
        }
    }

    public String getConfigValue() {
        return configValue;
    }

    public static class UnknownMaxRateRegistryTypeException extends IllegalArgumentException {
        UnknownMaxRateRegistryTypeException(String value) {
            super(String.format("Unknown max rate registry type: %s. Use one of: %s", value,
                    Arrays.stream(ConsumerMaxRateRegistryType.values())
                            .map(ConsumerMaxRateRegistryType::getConfigValue)
                            .collect(Collectors.joining(", "))));
        }
    }
}