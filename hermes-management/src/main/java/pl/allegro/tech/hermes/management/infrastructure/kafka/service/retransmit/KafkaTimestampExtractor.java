package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaSingleMessageReader;

class KafkaTimestampExtractor {

    private final TopicName topic;
    private final int partition;
    private final KafkaSingleMessageReader kafkaSingleMessageReader;
    private final JsonMessageContentWrapper messageContentWrapper;

    KafkaTimestampExtractor(TopicName topic, int partition, KafkaSingleMessageReader kafkaSingleMessageReader,
                            JsonMessageContentWrapper messageContentWrapper) {

        this.topic = topic;
        this.partition = partition;
        this.kafkaSingleMessageReader = kafkaSingleMessageReader;
        this.messageContentWrapper = messageContentWrapper;
    }

    public long extract(Long offset) {
        String message = kafkaSingleMessageReader.readMessage(topic, partition, offset);
        return messageContentWrapper.unwrapContent(message.getBytes()).getMessageMetadata().getTimestamp();
    }

}
