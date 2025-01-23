package pl.allegro.tech.hermes.tracker.consumers;

public class MessageMetadata {

  private final String messageId;
  private final String batchId;
  private final long offset;
  private final int partition;
  private final long partitionAssignmentTerm;
  private final String topic;
  private final String kafkaTopic;
  private final String subscription;
  private final long publishingTimestamp;
  private final long readingTimestamp;

  public MessageMetadata(
      String messageId,
      long offset,
      int partition,
      long partitionAssignmentTerm,
      String topic,
      String subscription,
      String kafkaTopic,
      long publishingTimestamp,
      long readingTimestamp) {
    this(
        messageId,
        "",
        offset,
        partition,
        partitionAssignmentTerm,
        topic,
        subscription,
        kafkaTopic,
        publishingTimestamp,
        readingTimestamp);
  }

  public MessageMetadata(
      String messageId,
      String batchId,
      long offset,
      int partition,
      long partitionAssignmentTerm,
      String topic,
      String subscription,
      String kafkaTopic,
      long publishingTimestamp,
      long readingTimestamp) {
    this.messageId = messageId;
    this.batchId = batchId;
    this.offset = offset;
    this.partition = partition;
    this.partitionAssignmentTerm = partitionAssignmentTerm;
    this.topic = topic;
    this.subscription = subscription;
    this.kafkaTopic = kafkaTopic;
    this.publishingTimestamp = publishingTimestamp;
    this.readingTimestamp = readingTimestamp;
  }

  public String getMessageId() {
    return messageId;
  }

  public long getOffset() {
    return offset;
  }

  public int getPartition() {
    return partition;
  }

  public long getPartitionAssignmentTerm() {
    return partitionAssignmentTerm;
  }

  public String getTopic() {
    return topic;
  }

  public String getSubscription() {
    return subscription;
  }

  public long getPublishingTimestamp() {
    return publishingTimestamp;
  }

  public long getReadingTimestamp() {
    return readingTimestamp;
  }

  public String getBatchId() {
    return batchId;
  }

  public String getKafkaTopic() {
    return kafkaTopic;
  }
}
