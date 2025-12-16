package pl.allegro.tech.hermes.tracker.consumers.deadletters;

public class DeadMessage {
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
  private final byte[] body;
  private final String rootCause;

  public DeadMessage(
      String messageId,
      String batchId,
      long offset,
      int partition,
      long partitionAssignmentTerm,
      String topic,
      String subscription,
      String kafkaTopic,
      long publishingTimestamp,
      long readingTimestamp,
      byte[] body,
      String rootCause) {
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
    this.body = body;
    this.rootCause = rootCause;
  }

  public DeadMessage(
      String messageId,
      long offset,
      int partition,
      long partitionAssignmentTerm,
      String topic,
      String subscription,
      String kafkaTopic,
      long publishingTimestamp,
      long readingTimestamp,
      byte[] body,
      String rootCause) {
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
        readingTimestamp,
        body,
        rootCause);
  }

  public String getMessageId() {
    return messageId;
  }

  public String getBatchId() {
    return batchId;
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

  public String getKafkaTopic() {
    return kafkaTopic;
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

  public byte[] getBody() {
    return body;
  }

  public String getRootCause() {
    return rootCause;
  }
}
