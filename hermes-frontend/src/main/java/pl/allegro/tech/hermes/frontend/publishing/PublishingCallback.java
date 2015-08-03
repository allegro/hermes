package pl.allegro.tech.hermes.frontend.publishing;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public interface PublishingCallback {

    void onUnpublished(Message message, Topic topic, Exception exception);

    void onPublished(Message message, Topic topic);

}
