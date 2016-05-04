package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.HttpResponder;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.RequestTimeoutLock;

public class HttpPublishingCallback implements PublishingCallback {

    private final HttpResponder httpResponder;
    private final RequestTimeoutLock requestTimeoutLock;

    public HttpPublishingCallback(HttpResponder httpResponder, RequestTimeoutLock requestTimeoutLock) {
        this.httpResponder = httpResponder;
        this.requestTimeoutLock = requestTimeoutLock;
    }

    @Override
    public void onUnpublished(Message message, Topic topic, Exception exception) {
        httpResponder.internalError(exception, "Broker seems to be down");
        requestTimeoutLock.unlock();
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        httpResponder.ok();
    }
}
