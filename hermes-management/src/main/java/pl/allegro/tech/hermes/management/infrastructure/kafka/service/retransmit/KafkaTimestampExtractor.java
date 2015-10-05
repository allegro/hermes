package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

class KafkaTimestampExtractor {

    private final Topic topic;
    private final KafkaTopic kafkaTopic;
    private final int partition;
    private final SingleMessageReader singleMessageReader;
    private final MessageContentWrapper messageContentWrapper;

    KafkaTimestampExtractor(Topic topic, KafkaTopic kafkaTopic, int partition, SingleMessageReader singleMessageReader,
                            MessageContentWrapper messageContentWrapper) {

        this.topic = topic;
        this.kafkaTopic = kafkaTopic;
        this.partition = partition;
        this.singleMessageReader = singleMessageReader;
        this.messageContentWrapper = messageContentWrapper;
    }

    public long extract(Long offset) {
        String message = singleMessageReader.readMessage(topic, kafkaTopic, partition, offset);
        return messageContentWrapper.unwrap(message.getBytes(), topic).getMessageMetadata().getTimestamp();
    }

}
