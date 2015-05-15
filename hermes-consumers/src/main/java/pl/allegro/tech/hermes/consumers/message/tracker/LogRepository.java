package pl.allegro.tech.hermes.consumers.message.tracker;


import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

public interface LogRepository {

    void logSuccessful(Message message, long timestamp, String topicName, String subscriptionName);
    void logFailed(Message message, long timestamp, String topicName, String subscriptionName, String reason);
    void logDiscarded(Message message, long timestamp, String topicName, String subscriptionName, String reason);
    void logInflight(Message message, long time, String qualifiedTopicName, String name);

}
