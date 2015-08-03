package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.HttpResponder;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;

public class HttpPublishingCallback implements PublishingCallback {

    private final HttpResponder httpResponder;

    public HttpPublishingCallback(HttpResponder httpResponder) {
        this.httpResponder = httpResponder;
    }

    @Override
    public void onUnpublished(Exception exception) {
        httpResponder.internalError(exception, "Broker seems to be down");
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        httpResponder.ok();
    }
}
