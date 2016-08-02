package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.broker;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker.BrokerOffsetCommitErrors;

import java.util.Optional;

public class CannotCommitOffsetToBrokerException extends RuntimeException {

    private BrokerOffsetCommitErrors errors;

    public CannotCommitOffsetToBrokerException(SubscriptionName subscriptionName, Exception cause) {
        super(String.format("Cannot commit offsets for subscription %s", subscriptionName), cause);
    }

    public CannotCommitOffsetToBrokerException(SubscriptionName subscriptionName, BrokerOffsetCommitErrors errors) {
        super(String.format("Cannot commit offsets for subscription %s. Received errors: %s",
                subscriptionName,
                errors.toString())
        );
        this.errors = errors;
    }

    public Optional<BrokerOffsetCommitErrors> getErrors() {
        return Optional.ofNullable(errors);
    }
}
