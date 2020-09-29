package pl.allegro.tech.hermes.management.infrastructure.kafka;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class BrokersClusterCommunicationException extends ManagementException {

    public BrokersClusterCommunicationException(Throwable t) {
        super(t);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.BROKERS_CLUSTER_COMMUNICATION_EXCEPTION;
    }
}
