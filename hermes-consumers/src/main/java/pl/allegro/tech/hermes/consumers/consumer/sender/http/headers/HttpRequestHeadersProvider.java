package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface HttpRequestHeadersProvider {

    HttpRequestHeaders getHeaders(Message message);

}
