package pl.allegro.tech.hermes.frontend.publishing;

import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public interface PublishingCallback {

    void onUnpublished(Message message, Topic topic, Exception exception);

    default void onUnpublished(Message message, Topic topic, RecordMetadata recordMetadata, Exception exception) {
        onUnpublished(message, topic, exception);
    }

    void onPublished(Message message, Topic topic);

    default void onPublished(Message message, Topic topic, RecordMetadata recordMetadata) {
        onPublished(message, topic);
    }
}
