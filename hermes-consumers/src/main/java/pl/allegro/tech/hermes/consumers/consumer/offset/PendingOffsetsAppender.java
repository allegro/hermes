package pl.allegro.tech.hermes.consumers.consumer.offset;

public interface PendingOffsetsAppender {

  void markAsProcessed(SubscriptionPartitionOffset subscriptionPartitionOffset);
}
