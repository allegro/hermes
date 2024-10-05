package pl.allegro.tech.hermes.consumers.consumer.message;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;

public class MessageConverter {

  public static MessageMetadata toMessageMetadata(Message message, Subscription subscription) {
    return new MessageMetadata(
        message.getId(),
        message.getOffset(),
        message.getPartition(),
        message.getPartitionAssignmentTerm(),
        message.getTopic(),
        subscription.getName(),
        message.getKafkaTopic().asString(),
        message.getPublishingTimestamp(),
        message.getReadingTimestamp());
  }

  public static MessageMetadata toMessageMetadata(
      Message message, Subscription subscription, String batchId) {
    return new MessageMetadata(
        message.getId(),
        batchId,
        message.getOffset(),
        message.getPartition(),
        message.getPartitionAssignmentTerm(),
        subscription.getQualifiedTopicName(),
        subscription.getName(),
        message.getKafkaTopic().asString(),
        message.getPublishingTimestamp(),
        message.getReadingTimestamp());
  }
}
