package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import java.time.Clock;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public class KafkaConsumerRecordToMessageConverter {

  private final Topic topic;
  private volatile Subscription subscription;
  private final Map<String, KafkaTopic> topics;
  private final MessageContentReader messageContentReader;
  private final KafkaHeaderExtractor kafkaHeaderExtractor;
  private final Clock clock;

  public KafkaConsumerRecordToMessageConverter(
      Topic topic,
      Subscription subscription,
      Map<String, KafkaTopic> topics,
      MessageContentReader messageContentReader,
      KafkaHeaderExtractor kafkaHeaderExtractor,
      Clock clock) {
    this.topic = topic;
    this.subscription = subscription;
    this.topics = topics;
    this.messageContentReader = messageContentReader;
    this.kafkaHeaderExtractor = kafkaHeaderExtractor;
    this.clock = clock;
  }

  public Message convertToMessage(
      ConsumerRecord<byte[], byte[]> record, long partitionAssignmentTerm) {
    KafkaTopic kafkaTopic = topics.get(record.topic());
    UnwrappedMessageContent unwrappedContent =
        messageContentReader.read(record, kafkaTopic.contentType());

    Map<String, String> externalMetadataFromBody =
        unwrappedContent.getMessageMetadata().getExternalMetadata();
    Map<String, String> externalMetadata =
        kafkaHeaderExtractor.extractExternalMetadata(record.headers(), externalMetadataFromBody);

    return new Message(
        kafkaHeaderExtractor.extractMessageId(record.headers()),
        topic.getQualifiedName(),
        unwrappedContent.getContent(),
        kafkaTopic.contentType(),
        unwrappedContent.getSchema(),
        record.timestamp(),
        clock.millis(),
        new PartitionOffset(kafkaTopic.name(), record.offset(), record.partition()),
        partitionAssignmentTerm,
        externalMetadata,
        subscription.getHeaders(),
        subscription.getName(),
        subscription.isSubscriptionIdentityHeadersEnabled());
  }

  public void update(Subscription newSubscription) {
    this.subscription = newSubscription;
  }
}
