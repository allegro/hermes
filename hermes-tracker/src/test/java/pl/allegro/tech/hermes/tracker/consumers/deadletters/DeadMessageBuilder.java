package pl.allegro.tech.hermes.tracker.consumers.deadletters;


import java.util.Random;
import java.util.UUID;

class DeadMessageBuilder {
    private String messageId;
    private String batchId;
    private long offset;
    private int partition;
    private long partitionAssignmentTerm;
    private String topic;
    private String kafkaTopic;
    private String subscription;
    private long publishingTimestamp;
    private long readingTimestamp;
    private byte[] body;
    private String rootCause;

    public DeadMessageBuilder setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }
    public DeadMessageBuilder setBatchId(String batchId) {
        this.batchId = batchId;
        return this;
    }
    public DeadMessageBuilder setOffset(long offset) {
        this.offset = offset;
        return this;
    }
    public DeadMessageBuilder setPartition(int partition) {
        this.partition = partition;
        return this;
    }
    public DeadMessageBuilder setPartitionAssignmentTerm(long partitionAssignmentTerm) {
        this.partitionAssignmentTerm = partitionAssignmentTerm;
        return this;
    }
    public DeadMessageBuilder setTopic(String topic) {
        this.topic = topic;
        return this;
    }
    public DeadMessageBuilder setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
        return this;
    }
    public DeadMessageBuilder setSubscription(String subscription) {
        this.subscription = subscription;
        return this;
    }
    public DeadMessageBuilder setPublishingTimestamp(long publishingTimestamp) {
        this.publishingTimestamp = publishingTimestamp;
        return this;
    }
    public DeadMessageBuilder setReadingTimestamp(long readingTimestamp) {
        this.readingTimestamp = readingTimestamp;
        return this;
    }
    public DeadMessageBuilder setBody(byte[] body) {
        this.body = body;
        return this;
    }
    public DeadMessageBuilder setRootCause(String rootCause) {
        this.rootCause = rootCause;
        return this;
    }
    public DeadMessage build() {
        return new DeadMessage(
                messageId,
                batchId,
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

    public static DeadMessage getRandomDeadMessage() {
        Random random = new Random();
        byte[] randomBytes = new byte[100];
        random.nextBytes(randomBytes);
        return new DeadMessageBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setBatchId(UUID.randomUUID().toString())
                .setOffset(random.nextLong())
                .setPartition(random.nextInt())
                .setPartitionAssignmentTerm(random.nextLong())
                .setTopic("pl.allegro.group.topic" + UUID.randomUUID().toString().substring(0, 8))
                .setSubscription("subscription")
                .setKafkaTopic("kafkaTopic")
                .setPublishingTimestamp(random.nextLong())
                .setReadingTimestamp(random.nextLong())
                .setBody(randomBytes)
                .setRootCause("rootCause" + UUID.randomUUID().toString().substring(0, 8))
                .build();
    }
}
