package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import com.google.common.base.Joiner;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

public class BrokerOffsetCommitErrors {

    private final Joiner.MapJoiner mapJoiner = Joiner.on(',').withKeyValueSeparator("=");
    private final Map<TopicPartition, Object> errors;

    public BrokerOffsetCommitErrors(Map<TopicPartition, Object> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return mapJoiner.join(errors);
    }
}
