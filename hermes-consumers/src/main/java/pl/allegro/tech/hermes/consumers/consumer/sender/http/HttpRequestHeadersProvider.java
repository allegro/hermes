package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface HttpRequestHeadersProvider {

    HttpRequestHeaders getHeaders(Message message);

}
