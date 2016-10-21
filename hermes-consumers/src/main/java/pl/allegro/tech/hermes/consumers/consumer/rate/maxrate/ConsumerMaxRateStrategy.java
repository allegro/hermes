package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import static java.lang.String.join;

class ConsumerMaxRateStrategy {

    static final String STRICT = "strict";
    static final String NEGOTIATED = "negotiated";

    static class UnknownMaxRateStrategyException extends InternalProcessingException {
        public UnknownMaxRateStrategyException() {
            super("Unknown max rate strategy. Use one of: " + join(", ", STRICT, NEGOTIATED));
        }
    }
}
