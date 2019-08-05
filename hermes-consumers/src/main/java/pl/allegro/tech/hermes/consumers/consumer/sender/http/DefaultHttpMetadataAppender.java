package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.api.Request;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

public class DefaultHttpMetadataAppender implements MetadataAppender<Request> {

    @Override
    public Request append(Request target, Message message) {
        message.getExternalMetadata().forEach(target::header);
        return target;
    }
}
