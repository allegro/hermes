package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.json.MessageContentWrapper;
import pl.allegro.tech.hermes.common.json.UnwrappedMessageContent;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaSingleMessageReader;

import java.util.Optional;

class KafkaTimestampExtractor {

    private final TopicName topic;
    private final int partition;
    private final KafkaSingleMessageReader kafkaSingleMessageReader;
    private final MessageContentWrapper messageContentWrapper;

    private static final String TIMESTAMP = "timestamp";

    KafkaTimestampExtractor(TopicName topic, int partition, KafkaSingleMessageReader kafkaSingleMessageReader,
                            MessageContentWrapper messageContentWrapper) {

        this.topic = topic;
        this.partition = partition;
        this.kafkaSingleMessageReader = kafkaSingleMessageReader;
        this.messageContentWrapper = messageContentWrapper;
    }

    public Optional<Long> extract(Long offset) {
        String message = kafkaSingleMessageReader.readMessage(topic, partition, offset);
        UnwrappedMessageContent unwrappedMessage = messageContentWrapper.unwrapContent(message.getBytes());
        return unwrappedMessage.getLongFromMetadata(TIMESTAMP);
    }

}
