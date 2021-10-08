package pl.allegro.tech.hermes.management.domain.dc;

import pl.allegro.tech.hermes.common.exception.HermesException;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

class ExceptionWrapper {
     static RuntimeException wrapInInternalProcessingExceptionIfNeeded(Exception toWrap,
                                                                       String command,
                                                                       String dcName) {
        if (toWrap instanceof HermesException) {
            return (HermesException) toWrap;
        }
        return new InternalProcessingException("Execution of command '" + command + "' failed on DC '" +
                dcName + "'.", toWrap);
    }
}
