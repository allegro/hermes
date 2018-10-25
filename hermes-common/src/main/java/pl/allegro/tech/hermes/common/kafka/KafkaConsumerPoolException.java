package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class KafkaConsumerPoolException extends HermesException {

    public KafkaConsumerPoolException(String message, Throwable t) {
        super(message, t);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SIMPLE_CONSUMER_POOL_EXCEPTION;
    }

}
