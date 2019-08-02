package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import static java.lang.String.join;

class ConsumerMaxRateRegistryType {

    static final String HIERARCHICAL = "hierarchical";
    static final String FLAT_BINARY = "flat-binary";

    static class UnknownMaxRateRegistryException extends InternalProcessingException {
        UnknownMaxRateRegistryException() {
            super("Unknown max rate registry type. Use one of: " + join(", ", HIERARCHICAL, FLAT_BINARY));
        }
    }
}
