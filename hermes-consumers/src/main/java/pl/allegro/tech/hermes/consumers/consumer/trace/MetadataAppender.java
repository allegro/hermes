package pl.allegro.tech.hermes.consumers.consumer.trace;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface MetadataAppender<T> {

  T append(T target, Message message);
}
