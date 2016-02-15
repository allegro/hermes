package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaRawMessageReader;

class KafkaTimestampExtractor {

    private final Topic topic;
    private final KafkaTopic kafkaTopic;
    private final int partition;
    private final KafkaRawMessageReader kafkaRawMessageReader;
    private final MessageContentWrapper messageContentWrapper;

    KafkaTimestampExtractor(Topic topic, KafkaTopic kafkaTopic, int partition, KafkaRawMessageReader kafkaRawMessageReader,
                            MessageContentWrapper messageContentWrapper) {

        this.topic = topic;
        this.kafkaTopic = kafkaTopic;
        this.partition = partition;
        this.kafkaRawMessageReader = kafkaRawMessageReader;
        this.messageContentWrapper = messageContentWrapper;
    }

    public long extract(Long offset) {
        byte[] message = kafkaRawMessageReader.readMessage(kafkaTopic, partition, offset);
        return messageContentWrapper.unwrap(message, topic, kafkaTopic.contentType()).getMessageMetadata().getTimestamp();
    }

}
