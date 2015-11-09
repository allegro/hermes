package pl.allegro.tech.hermes.consumers.consumer.trace;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface TraceAppender<T> {

    T appendTraceInfo(T target, Message message);
}
