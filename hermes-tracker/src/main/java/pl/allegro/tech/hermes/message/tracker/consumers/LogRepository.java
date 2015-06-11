package pl.allegro.tech.hermes.message.tracker.consumers;


public interface LogRepository {

    void logSuccessful(MessageMetadata message, long timestamp, String topicName, String subscriptionName);
    void logFailed(MessageMetadata message, long timestamp, String topicName, String subscriptionName, String reason);
    void logDiscarded(MessageMetadata message, long timestamp, String topicName, String subscriptionName, String reason);
    void logInflight(MessageMetadata message, long time, String qualifiedTopicName, String name);

}
