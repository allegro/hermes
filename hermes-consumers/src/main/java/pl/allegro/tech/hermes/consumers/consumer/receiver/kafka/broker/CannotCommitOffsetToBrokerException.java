package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker;

import pl.allegro.tech.hermes.common.broker.BrokerOffsetCommitErrors;

public class CannotCommitOffsetToBrokerException extends RuntimeException {

    private final BrokerOffsetCommitErrors errors;

    public CannotCommitOffsetToBrokerException(BrokerOffsetCommitErrors errors) {
        super(String.format("Cannot commit offset, error codes: %s", errors.toString()));
        this.errors = errors;
    }

    public BrokerOffsetCommitErrors getErrors() {
        return errors;
    }
}
