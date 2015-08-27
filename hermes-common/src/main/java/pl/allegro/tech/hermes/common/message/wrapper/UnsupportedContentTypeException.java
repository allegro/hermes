package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class UnsupportedContentTypeException extends InternalProcessingException {

    public UnsupportedContentTypeException(Topic topic) {
        super(String.format("Unsupported content type %s for topic %s", topic.getContentType(), topic.getQualifiedName()));
    }

}
