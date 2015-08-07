package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

class KafkaTimestampExtractor {

    private final Topic topic;
    private final int partition;
    private final SingleMessageReader singleMessageReader;
    private final JsonMessageContentWrapper messageContentWrapper;

    KafkaTimestampExtractor(Topic topic, int partition, SingleMessageReader singleMessageReader,
                            JsonMessageContentWrapper messageContentWrapper) {

        this.topic = topic;
        this.partition = partition;
        this.singleMessageReader = singleMessageReader;
        this.messageContentWrapper = messageContentWrapper;
    }

    public long extract(Long offset) {
        String message = singleMessageReader.readMessage(topic, partition, offset);
        return messageContentWrapper.unwrapContent(message.getBytes()).getMessageMetadata().getTimestamp();
    }

}
