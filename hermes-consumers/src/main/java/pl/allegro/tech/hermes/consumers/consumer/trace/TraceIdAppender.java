package pl.allegro.tech.hermes.consumers.consumer.trace;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface TraceIdAppender<T> {

    T appendTraceId(T target, Message message);
}
