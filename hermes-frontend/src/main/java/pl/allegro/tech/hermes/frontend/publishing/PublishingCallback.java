package pl.allegro.tech.hermes.frontend.publishing;

import pl.allegro.tech.hermes.api.Topic;

public interface PublishingCallback {

    void onUnpublished(Exception exception);

    void onPublished(Message message, Topic topic);

}
