package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface HttpHeadersProvider {

    HttpRequestHeaders getHeaders(Message message);

}
