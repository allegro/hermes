package pl.allegro.tech.hermes.frontend.publishing;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.metadata.ProduceMetadata;

import java.util.function.Supplier;

public interface PublishingCallback {

    void onUnpublished(Message message, Topic topic, Exception exception);

    default void onUnpublished(Message message, Topic topic, Supplier<ProduceMetadata> produceMetadata, Exception exception) {
        onUnpublished(message, topic, exception);
    }

    void onPublished(Message message, Topic topic);

    default void onPublished(Message message, Topic topic, Supplier<ProduceMetadata> produceMetadata) {
        onPublished(message, topic);
    }
}
