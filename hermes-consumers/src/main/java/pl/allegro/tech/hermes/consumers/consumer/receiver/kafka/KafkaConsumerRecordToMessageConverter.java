package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.time.Clock;
import java.util.Map;

public class KafkaConsumerRecordToMessageConverter {

    private final Topic topic;
    private volatile Subscription subscription;
    private final Map<String, KafkaTopic> topics;
    private final MessageContentReader messageContentReader;
    private final Clock clock;

    public KafkaConsumerRecordToMessageConverter(Topic topic,
                                                 Subscription subscription,
                                                 Map<String, KafkaTopic> topics,
                                                 MessageContentReader messageContentReader,
                                                 Clock clock) {
        this.topic = topic;
        this.subscription = subscription;
        this.topics = topics;
        this.messageContentReader = messageContentReader;
        this.clock = clock;
    }

    public Message convertToMessage(ConsumerRecord<byte[], byte[]> record, long partitionAssignmentTerm) {
        KafkaTopic kafkaTopic = topics.get(record.topic());
        UnwrappedMessageContent unwrappedContent = messageContentReader.read(record, kafkaTopic.contentType());
        return new Message(
                unwrappedContent.getMessageMetadata().getId(),
                topic.getQualifiedName(),
                unwrappedContent.getContent(),
                kafkaTopic.contentType(),
                unwrappedContent.getSchema(),
                unwrappedContent.getMessageMetadata().getTimestamp(),
                clock.millis(),
                new PartitionOffset(kafkaTopic.name(), record.offset(), record.partition()),
                partitionAssignmentTerm,
                unwrappedContent.getMessageMetadata().getExternalMetadata(),
                subscription.getHeaders(),
                subscription.getName(),
                subscription.isSubscriptionIdentityHeadersEnabled()
        );
    }

    public void update(Subscription newSubscription) {
        this.subscription = newSubscription;
    }

}
