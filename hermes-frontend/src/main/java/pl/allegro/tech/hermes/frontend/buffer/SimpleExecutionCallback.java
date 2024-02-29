package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.collect.Lists;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.util.List;

public class SimpleExecutionCallback implements PublishingCallback {

    private final List<PublishingCallback> callbacks;

    public SimpleExecutionCallback(PublishingCallback... callbacks) {
        this.callbacks = Lists.newArrayList(callbacks);
    }

    @Override
    public void onUnpublished(Message message, Topic topic, Exception exception) {
        callbacks.forEach(c -> c.onUnpublished(message, topic, exception));
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        callbacks.forEach(c -> c.onPublished(message, topic));
    }

    @Override
    public void onEachPublished(Message message, Topic topic, String datacenter) {
        callbacks.forEach(c -> c.onEachPublished(message, topic, datacenter));
    }
}
