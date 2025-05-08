package pl.allegro.tech.hermes.consumers.consumer.message;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadMessage;

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

  public static DeadMessage toDeadMessage(Message message, Subscription subscription, MessageSendingResult messageSendingResult) {
     if (messageSendingResult.getRootCause() != null) {
       return new DeadMessage(
               message.getId(),
               message.getOffset(),
               message.getPartition(),
               message.getPartitionAssignmentTerm(),
               message.getTopic(),
               subscription.getName(),
               message.getKafkaTopic().asString(),
               message.getPublishingTimestamp(),
               message.getReadingTimestamp(),
               message.getData(),
               messageSendingResult.getRootCause()
       );
     } else {
       StringBuilder cause= new StringBuilder(String.format("Status code : %s", messageSendingResult.getStatusCode()));
       if (messageSendingResult.getLogInfo() != null) {
          messageSendingResult.getLogInfo().stream()
                  .filter(logInfo -> logInfo.getRootCause() != null)
                  .forEach( logInfo -> cause.append(String.format("\n%s, %s", logInfo.getRootCause(), logInfo))
          );
       }
       return new DeadMessage(
               message.getId(),
               message.getOffset(),
               message.getPartition(),
               message.getPartitionAssignmentTerm(),
               message.getTopic(),
               subscription.getName(),
               message.getKafkaTopic().asString(),
               message.getPublishingTimestamp(),
               message.getReadingTimestamp(),
               message.getData(),
               cause.toString()
       );
     }
  }

}
